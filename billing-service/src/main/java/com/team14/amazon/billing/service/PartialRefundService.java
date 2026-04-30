package com.team14.amazon.billing.service;

import com.team14.amazon.billing.cache.BillingCacheInvalidationService;
import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.observer.MongoEventLogger;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.strategy.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PartialRefundService {

    private final TransactionRepository transactionRepository;
    private final MongoEventLogger mongoEventLogger;
    private final BillingCacheInvalidationService cacheInvalidationService;

    public PartialRefundService(TransactionRepository transactionRepository,
                                MongoEventLogger mongoEventLogger,
                                BillingCacheInvalidationService cacheInvalidationService) {
        this.transactionRepository = transactionRepository;
        this.mongoEventLogger = mongoEventLogger;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional
    public Transaction processPartialRefund(Long transactionId, RefundRequest request) {

        // 1. Find transaction — 404 if not found
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId));

        // 2. Must be COMPLETED
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only COMPLETED transactions can be refunded");
        }

        // 3. Load order items via cross-service native SQL
        List<Object[]> rawItems = transactionRepository.findOrderItemsByOrderId(transaction.getOrderId());
        List<Map<String, Object>> orderItems = rawItems.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id",              ((Number) row[0]).longValue());
            item.put("productId",       ((Number) row[1]).longValue());
            item.put("quantity",        ((Number) row[2]).intValue());
            item.put("priceAtPurchase", ((Number) row[3]).doubleValue());
            return item;
        }).toList();

        // 4. Validate orderItemIds when refundAll=false
        if (!Boolean.TRUE.equals(request.getRefundAll())) {
            List<Long> requestedIds = request.getOrderItemIds();
            if (requestedIds == null || requestedIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "orderItemIds must not be empty when refundAll=false");
            }
            Long matched = transactionRepository.countItemsBelongingToOrder(
                    requestedIds, transaction.getOrderId());
            if (matched != requestedIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "One or more orderItemIds do not belong to this transaction's order");
            }
        }

        // 5. Select strategy
        RefundStrategySelector selector = new RefundStrategySelector(orderItems);
        RefundStrategy strategy = selector.select(transaction, request);
        RefundResult result = strategy.calculateRefund(transaction, request);

        // 6. NoRefundStrategy — log REFUND_DENIED, invalidate, throw 400
        if (strategy instanceof NoRefundStrategy) {
            mongoEventLogger.onEvent("REFUND_DENIED", buildPayload(transaction, strategy, result, request));
            cacheInvalidationService.evictTransactionCaches(transactionId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "return window expired");
        }

        // 7. Apply refund
        transaction.setStatus(TransactionStatus.REFUNDED);

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        details.put("refundAmount",   result.getAmount());
        details.put("refundedItems",  buildRefundedItemsList(orderItems, result));
        details.put("refundStrategy", strategy.getClass().getSimpleName());
        details.put("refundReason",   request.getReason());
        details.put("refundedAt",     LocalDateTime.now().toString());
        transaction.setTransactionDetails(details);

        Transaction saved = transactionRepository.save(transaction);

        // 8. Log REFUNDED via Observer
        mongoEventLogger.onEvent("REFUNDED", buildPayload(transaction, strategy, result, request));

        // 9. Invalidate caches
        cacheInvalidationService.evictTransactionCaches(transactionId);

        return saved;
    }

    private List<Map<String, Object>> buildRefundedItemsList(
            List<Map<String, Object>> orderItems, RefundResult result) {
        return orderItems.stream()
                .filter(item -> result.getRefundedItemIds()
                        .contains(((Number) item.get("id")).longValue()))
                .map(item -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("orderItemId", item.get("id"));
                    entry.put("quantity",    item.get("quantity"));
                    entry.put("amount",
                            ((Number) item.get("priceAtPurchase")).doubleValue()
                            * ((Number) item.get("quantity")).intValue());
                    return entry;
                }).toList();
    }

    private Map<String, Object> buildPayload(Transaction transaction, RefundStrategy strategy,
                                              RefundResult result, RefundRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId",  transaction.getId());
        payload.put("method",         transaction.getMethod() != null ? transaction.getMethod().name() : null);
        payload.put("amount",         transaction.getAmount());
        payload.put("refundAmount",   result.getAmount());
        payload.put("refundStrategy", strategy.getClass().getSimpleName());
        payload.put("reason",         request.getReason());
        payload.put("orderItemIds",   request.getOrderItemIds());
        payload.put("timestamp",      LocalDateTime.now());
        return payload;
    }
}