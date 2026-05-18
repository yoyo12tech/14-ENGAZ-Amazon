package com.team14.amazon.billing.service;

import com.team14.amazon.billing.cache.BillingCacheInvalidationService;
import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.exception.ConflictException;
import com.team14.amazon.billing.messaging.publishers.PaymentEventPublisher;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.observer.MongoEventLogger;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.strategy.NoRefundStrategy;
import com.team14.amazon.billing.strategy.RefundResult;
import com.team14.amazon.billing.strategy.RefundStrategy;
import com.team14.amazon.billing.strategy.RefundStrategySelector;
import com.team14.amazon.contracts.dto.OrderDetailsDTO;
import com.team14.amazon.contracts.dto.OrderItemDTO;
import com.team14.amazon.contracts.events.PaymentRefundedEvent;
import com.team14.amazon.contracts.feign.OrderServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartialRefundService {

    private final TransactionRepository transactionRepository;
    private final MongoEventLogger mongoEventLogger;
    private final BillingCacheInvalidationService cacheInvalidationService;
    private final OrderServiceClient orderServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;

    public PartialRefundService(TransactionRepository transactionRepository,
                                MongoEventLogger mongoEventLogger,
                                BillingCacheInvalidationService cacheInvalidationService,
                                OrderServiceClient orderServiceClient,
                                PaymentEventPublisher paymentEventPublisher) {
        this.transactionRepository = transactionRepository;
        this.mongoEventLogger = mongoEventLogger;
        this.cacheInvalidationService = cacheInvalidationService;
        this.orderServiceClient = orderServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public Transaction processPartialRefund(Long transactionId, RefundRequest request) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only COMPLETED transactions can be refunded");
        }

        OrderDetailsDTO detailsDto = orderServiceClient.getOrderDetails(transaction.getOrderId());
        if (detailsDto == null || detailsDto.getItems() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order items not found for this transaction");
        }

        List<Map<String, Object>> orderItems = detailsDto.getItems().stream()
                .map(this::orderItemToMap)
                .toList();

        if (!Boolean.TRUE.equals(request.getRefundAll())) {
            List<Long> requestedIds = request.getOrderItemIds();
            if (requestedIds == null || requestedIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "orderItemIds must not be empty when refundAll=false");
            }
            Set<Long> validIds = detailsDto.getItems().stream()
                    .map(OrderItemDTO::id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Long reqId : requestedIds) {
                if (reqId == null || !validIds.contains(reqId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "One or more orderItemIds do not belong to this transaction's order");
                }
            }
        }

        RefundStrategySelector selector = new RefundStrategySelector(orderItems);
        RefundStrategy strategy = selector.select(transaction, request);
        RefundResult result = strategy.calculateRefund(transaction, request);

        if (strategy instanceof NoRefundStrategy) {
            mongoEventLogger.onEvent("REFUND_DENIED", buildPayload(transaction, strategy, result, request));
            cacheInvalidationService.evictTransactionCaches(transactionId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "return window expired");
        }

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        details.put("refundAmount", result.getAmount());
        details.put("refundedItems", buildRefundedItemsList(orderItems, result));
        details.put("refundStrategy", strategy.getClass().getSimpleName());
        details.put("refundReason", request.getReason());
        details.put("refundedAt", LocalDateTime.now().toString());

        int updated = transactionRepository.updateStatusAtomically(
                transactionId, TransactionStatus.COMPLETED, TransactionStatus.REFUNDED);
        if (updated == 0) {
            throw new ConflictException("Could not transition transaction to REFUNDED; status changed concurrently");
        }

        Transaction fresh = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction not found: " + transactionId));
        fresh.setTransactionDetails(details);
        Transaction saved = transactionRepository.save(fresh);

        mongoEventLogger.onEvent("REFUNDED", buildPayload(saved, strategy, result, request));

        paymentEventPublisher.publishRefunded(
                new PaymentRefundedEvent(saved.getId(), saved.getOrderId(), result.getAmount()));

        cacheInvalidationService.evictTransactionCaches(transactionId);

        return saved;
    }

    private Map<String, Object> orderItemToMap(OrderItemDTO dto) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", dto.id());
        item.put("productId", dto.productId());
        item.put("quantity", dto.quantity());
        item.put("priceAtPurchase", dto.priceAtPurchase());
        return item;
    }

    private List<Map<String, Object>> buildRefundedItemsList(
            List<Map<String, Object>> orderItems, RefundResult result) {
        return orderItems.stream()
                .filter(item -> result.getRefundedItemIds()
                        .contains(((Number) item.get("id")).longValue()))
                .map(item -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("orderItemId", item.get("id"));
                    entry.put("quantity", item.get("quantity"));
                    entry.put("amount",
                            ((Number) item.get("priceAtPurchase")).doubleValue()
                                    * ((Number) item.get("quantity")).intValue());
                    return entry;
                })
                .toList();
    }

    private Map<String, Object> buildPayload(Transaction transaction, RefundStrategy strategy,
                                             RefundResult result, RefundRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transaction.getId());
        payload.put("method", transaction.getMethod() != null ? transaction.getMethod().name() : null);
        payload.put("amount", transaction.getAmount());
        payload.put("refundAmount", result.getAmount());
        payload.put("refundStrategy", strategy.getClass().getSimpleName());
        payload.put("reason", request.getReason());
        payload.put("orderItemIds", request.getOrderItemIds());
        payload.put("timestamp", LocalDateTime.now());
        return payload;
    }
}
