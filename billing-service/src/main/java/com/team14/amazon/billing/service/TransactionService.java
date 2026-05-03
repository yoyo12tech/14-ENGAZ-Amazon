package com.team14.amazon.billing.service;

import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.cache.BillingCacheNames;
import com.team14.amazon.billing.model.*;
import com.team14.amazon.billing.observer.EntityObserver;
import com.team14.amazon.billing.dto.RevenueReportDTO;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.dto.UserTransactionSummaryDTO;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import com.team14.amazon.billing.repository.VoucherRepository;
import com.team14.amazon.billing.cache.BillingCacheInvalidationService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.billing.dto.ProcessTransactionRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.team14.amazon.billing.dto.AppliedVoucherDetail;
import com.team14.amazon.billing.dto.TransactionDetailsDTO;
import com.team14.amazon.billing.adapter.ObjectArrayDtoAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.team14.amazon.billing.dto.CategoryRevenueDTO;
import com.team14.amazon.billing.repository.CategoryRevenueProjection;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionVoucherRepository transactionVoucherRepository;
    private final List<EntityObserver> observers;
    private final BillingCacheInvalidationService cacheInvalidationService;

    public TransactionService(TransactionRepository transactionRepository, VoucherRepository voucherRepository,
            TransactionVoucherRepository transactionVoucherRepository,
            List<EntityObserver> observers, BillingCacheInvalidationService cacheInvalidationService) {
        this.transactionRepository = transactionRepository;
        this.voucherRepository = voucherRepository;
        this.transactionVoucherRepository = transactionVoucherRepository;
        this.observers = new ArrayList<>(observers);
        this.cacheInvalidationService = cacheInvalidationService;
    }

    public void register(EntityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private Map<String, Object> auditPayload(Long transactionId, String method, Double amount,
            Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transactionId);
        payload.put("method", method);
        payload.put("amount", amount);
        payload.put("details", details);
        return payload;
    }

    public Transaction createTransaction(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);

        Map<String, Object> details = new HashMap<>();
        details.put("source", "CRUD_API");
        String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;

        notifyObservers("SYSTEM_CREATE", auditPayload(saved.getId(), methodName, saved.getAmount(), details));
        cacheInvalidationService.evictTransactionCaches(saved.getId());

        return saved;
    }

    @Cacheable(cacheNames = BillingCacheNames.TRANSACTION, key = "T(com.team14.amazon.billing.cache.BillingCacheNames).entityKey(#id)")
    public Transaction findTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    }

    public List<Transaction> findAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction updateTransaction(Transaction transaction) {
        if (!transactionRepository.existsById(transaction.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        Transaction saved = transactionRepository.save(transaction);

        Map<String, Object> details = new HashMap<>();
        details.put("source", "CRUD_API");
        String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;

        notifyObservers("SYSTEM_UPDATE", auditPayload(saved.getId(), methodName, saved.getAmount(), details));
        cacheInvalidationService.evictTransactionCaches(saved.getId());

        return saved;
    }

    public void deleteTransaction(Long id) {
        Transaction existing = transactionRepository.findById(id).orElse(null);
        transactionRepository.deleteById(id);
        cacheInvalidationService.evictTransactionCaches(id);

        if (existing != null) {
            Map<String, Object> details = new HashMap<>();
            details.put("source", "CRUD_API");
            String methodName = existing.getMethod() != null ? existing.getMethod().name() : null;

            notifyObservers("SYSTEM_DELETE", auditPayload(id, methodName, existing.getAmount(), details));
        }
    }

    public Transaction findByOrderId(Long id) {
        return transactionRepository.findByOrderId(id);
    }

    public Transaction findByUserId(Long id) {
        return transactionRepository.findByUserId(id);
    }

    public List<Transaction> findByMethod(TransactionMethod method) {
        return transactionRepository.findByMethod(method);
    }

    public List<Transaction> findByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    @Cacheable(
            cacheNames = BillingCacheNames.S5_F6,
            key = "'start:' + #startDateStr + '|end:' + #endDateStr"
    )
    public RevenueReportDTO getRevenueReport(String startDateStr, String endDateStr) {
        LocalDateTime startDate;
        LocalDateTime endDate;
        try {
            startDate = LocalDate.parse(startDateStr).atStartOfDay();
            endDate = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format", e);
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date");
        }

        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(startDate, endDate);

        double totalRevenue = 0.0;
        long totalTransactions = 0;
        double refundedAmount = 0.0;
        long refundCount = 0;

        for (Transaction t : transactions) {
            if (t.getStatus() == TransactionStatus.COMPLETED) {
                totalRevenue += t.getAmount();
                totalTransactions++;
            } else if (t.getStatus() == TransactionStatus.REFUNDED) {
                refundedAmount += t.getAmount();
                refundCount++;
            }
        }

        double averageTransaction = 0.0;
        if (totalTransactions > 0) {
            averageTransaction = totalRevenue / totalTransactions;
        }

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .averageTransaction(averageTransaction)
                .refundedAmount(refundedAmount)
                .refundCount(refundCount)
                .build();
    }

    @Cacheable(cacheNames = BillingCacheNames.S5_F3, key = "'userId:' + #userId")
    public UserTransactionSummaryDTO getUserTransactionSummary(Long userId) {
        if (!transactionRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Object[]> results = transactionRepository.getCompletedTransactionSummaryByUser(userId,
                TransactionStatus.COMPLETED);

        ObjectArrayDtoAdapter adapter = new ObjectArrayDtoAdapter();
        return adapter.adapt(userId, results);
    }

    @Cacheable(
            cacheNames = BillingCacheNames.S5_F1,
            key = "'status:' + (#statusStr == null ? 'all' : #statusStr) + '|start:' + (#startDateStr == null ? 'all' : #startDateStr) + '|end:' + (#endDateStr == null ? 'all' : #endDateStr)"
    )
    public List<Transaction> searchTransactions(String statusStr, String startDateStr, String endDateStr) {
        List<Transaction> transactions = transactionRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "createdAt"));

        return transactions.stream().filter(t -> {
            boolean matches = true;
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                matches = matches && t.getStatus().name().equalsIgnoreCase(statusStr);
            }
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                LocalDateTime start;
                try {
                    // Try parsing as ISO Date (2026-04-10)
                    start = LocalDate.parse(startDateStr).atStartOfDay();
                } catch (Exception e) {
                    // Fallback for full DateTime strings
                    start = LocalDateTime.parse(startDateStr);
                }
                matches = matches && !t.getCreatedAt().isBefore(start);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                java.time.LocalDateTime end;
                try {
                    end = java.time.LocalDate.parse(endDateStr).atTime(java.time.LocalTime.MAX);
                } catch (Exception e) {
                    end = java.time.LocalDateTime.parse(endDateStr);
                }
                matches = matches && !t.getCreatedAt().isAfter(end);
            }
            return matches;
        }).toList();
    }

    @Transactional
    public Transaction processOrderTransaction(Long orderId, ProcessTransactionRequest request,
            boolean simulateFailure) {
        String orderStatus = transactionRepository.findOrderStatusByOrderId(orderId);
        if (orderStatus == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Order not found: " + orderId);
        }

        if (!orderStatus.equalsIgnoreCase("DELIVERED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DELIVERED orders can be processed");
        }

        Transaction existing = transactionRepository.findByOrderId(orderId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No transaction found for order: " + orderId);
        }

        if (existing.getStatus() == TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        TransactionMethod effectiveMethod = existing.getMethod();
        if (request.getMethod() != null) {
            effectiveMethod = request.getMethod();
        }

        if (!simulateFailure) {
            Map<String, Object> createdEventDetails = new HashMap<>();
            createdEventDetails.put("orderId", orderId);
            String methodName = effectiveMethod != null ? effectiveMethod.name() : null;
            notifyObservers("CREATED",
                    auditPayload(existing.getId(), methodName, existing.getAmount(), createdEventDetails));
        }

        existing.setMethod(effectiveMethod);
        if (simulateFailure) {
            existing.setStatus(TransactionStatus.FAILED);
            Map<String, Object> details = new HashMap<>();
            if (existing.getTransactionDetails() != null) {
                details.putAll(existing.getTransactionDetails());
            }
            details.put("simulateFailure", true);
            existing.setTransactionDetails(details);
            Transaction saved = transactionRepository.save(existing);
            Map<String, Object> failedDetails = new HashMap<>();
            failedDetails.put("orderId", orderId);
            failedDetails.put("reason", "SIMULATED_FAILURE");
            String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;
            notifyObservers("FAILED", auditPayload(saved.getId(), methodName, saved.getAmount(), failedDetails));
            cacheInvalidationService.evictTransactionCaches(saved.getId());
            return saved;
        }

        existing.setStatus(TransactionStatus.COMPLETED);

        Map<String, Object> details = new HashMap<>();
        if (existing.getTransactionDetails() != null) {
            details.putAll(existing.getTransactionDetails());
        }
        details.put("gatewayResponse", "approved");
        if (request.getCardLastFour() != null) {
            details.put("cardLastFour", request.getCardLastFour());
        }
        existing.setTransactionDetails(details);

        Transaction saved = transactionRepository.save(existing);
        Map<String, Object> completedDetails = new HashMap<>();
        completedDetails.put("orderId", orderId);
        completedDetails.put("gatewayResponse", "approved");
        String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;
        notifyObservers("COMPLETED", auditPayload(saved.getId(), methodName, saved.getAmount(), completedDetails));
        cacheInvalidationService.evictTransactionCaches(saved.getId());
        return saved;
    }

    public Transaction processRefund(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only COMPLETED transactions are eligible for a refund.");
        }

        transaction.setStatus(TransactionStatus.REFUNDED);

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        details.put("refundReason", reason);
        details.put("refundedAt", LocalDateTime.now().toString());
        transaction.setTransactionDetails(details);

        Transaction saved = transactionRepository.save(transaction);
        Map<String, Object> refundDetails = new HashMap<>();
        refundDetails.put("refundReason", reason);
        String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;
        notifyObservers("REFUNDED", auditPayload(saved.getId(), methodName, saved.getAmount(), refundDetails));
        cacheInvalidationService.evictTransactionCaches(saved.getId());
        return saved;
    }

    @Transactional
    public Transaction applyVoucher(Long transactionId, Long voucherId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaction with id: " + transactionId + " was not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot apply voucher to a completed/cancelled transaction");
        }
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Voucher with id: " + voucherId + " was not found"));

        int currentUses = voucher.getCurrentUses() != null ? voucher.getCurrentUses() : 0;
        int maxUses = voucher.getMaxUses() != null ? voucher.getMaxUses() : Integer.MAX_VALUE;
        double txAmount = transaction.getAmount() != null ? transaction.getAmount() : 0.0;
        double discValue = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : 0.0;
        if (!voucher.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is inactive");
        }
        if (voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher has expired");
        }
        if (currentUses >= maxUses) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher usage limit reached");
        }

        if (transactionVoucherRepository.existsByTransactionIdAndVoucherId(transactionId, voucherId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voucher already applied");
        }

        double discount;
        discount = (voucher.getDiscountType() == DiscountType.PERCENTAGE) ? (txAmount * discValue / 100) : discValue;
        discount = Math.min(discount, txAmount);

        TransactionVoucher transactionVoucher = new TransactionVoucher();
        transactionVoucher.setTransaction(transaction);
        transactionVoucher.setVoucher(voucher);
        transactionVoucher.setDiscountApplied(discount);
        transactionVoucher.setAppliedAt(LocalDateTime.now());

        voucher.setCurrentUses(currentUses + 1);

        voucherRepository.save(voucher);
        transactionVoucherRepository.save(transactionVoucher);
        if (transaction.getTransactionVouchers() == null) {
            transaction.setTransactionVouchers(new ArrayList<>());
        }
        transaction.getTransactionVouchers().add(transactionVoucher);
        Transaction saved = transactionRepository.save(transaction);

        Map<String, Object> voucherDetails = new HashMap<>();
        voucherDetails.put("voucherId", voucherId);
        voucherDetails.put("discountApplied", discount);
        String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;
        notifyObservers("VOUCHER_APPLIED", auditPayload(saved.getId(), methodName, saved.getAmount(), voucherDetails));
        cacheInvalidationService.evictTransactionAndVoucherCaches(saved.getId(), voucherId);
        return saved;
    }

    public Transaction retryTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        if (transaction.getStatus() != TransactionStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only FAILED transactions can be retried");
        }

        transaction.setStatus(TransactionStatus.COMPLETED);

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        int retryAttempt = 0;
        if (details.get("retryAttempt") instanceof Number) {
            retryAttempt = ((Number) details.get("retryAttempt")).intValue();
        }
        details.put("retryAttempt", retryAttempt + 1);
        details.put("gatewayResponse", "approved");
        transaction.setTransactionDetails(details);

        Transaction saved = transactionRepository.save(transaction);
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("retryAttempt", retryAttempt + 1);
        String methodName = saved.getMethod() != null ? saved.getMethod().name() : null;
        notifyObservers("RETRY_COMPLETED", auditPayload(saved.getId(), methodName, saved.getAmount(), eventDetails));
        cacheInvalidationService.evictTransactionCaches(saved.getId());
        return saved;
    }

    @Cacheable(cacheNames = BillingCacheNames.S5_F8, key = "'transactionId:' + #transactionId")
    public TransactionDetailsDTO getTransactionDetails(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        List<AppliedVoucherDetail> appliedVouchers = transaction.getTransactionVouchers() == null
                ? List.of()
                : transaction.getTransactionVouchers().stream()
                        .map(tv -> new AppliedVoucherDetail(
                                tv.getVoucher().getCode(),
                                tv.getVoucher().getDiscountType(),
                                tv.getDiscountApplied(),
                                tv.getAppliedAt()))
                        .toList();

        double totalDiscount = appliedVouchers.stream()
                .mapToDouble(AppliedVoucherDetail::getDiscountApplied)
                .sum();

        double finalAmount = transaction.getAmount() - totalDiscount;

        return TransactionDetailsDTO.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .userId(transaction.getUserId())
                .originalAmount(transaction.getAmount())
                .method(transaction.getMethod())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .transactionDetails(transaction.getTransactionDetails())
                .appliedVouchers(appliedVouchers)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
    }

    @Cacheable(cacheNames = BillingCacheNames.S5_F9, key = "'limit:' + #limit")
    public List<VoucherUsageDTO> getTopUsedVouchers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionVoucherRepository.findTopUsedVouchers(pageable);
    }

    @Cacheable(value = BillingCacheNames.S5_F10, key = "#startDate.toString() + '_' + #endDate.toString()")
    public List<CategoryRevenueDTO> getCategoryRevenue(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<CategoryRevenueProjection> rawData = transactionRepository.findCategoryRevenueRawData(start, end);

        class CategoryAgg {
            String category;
            double grossRevenue = 0.0;
            double refundedRevenue = 0.0;
            Set<Long> txIds = new HashSet<>();
            Set<Long> refundTxIds = new HashSet<>();
            CategoryAgg(String c) { category = c; }
        }

        Map<String, CategoryAgg> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        for (CategoryRevenueProjection row : rawData) {
            String cat = row.getCategory();
            map.putIfAbsent(cat, new CategoryAgg(cat));
            CategoryAgg agg = map.get(cat);

            agg.txIds.add(row.getTransactionId());
            double lineGross = row.getQuantity() * row.getPriceAtPurchase();
            agg.grossRevenue += lineGross;

            if ("REFUNDED".equals(row.getTransactionStatus())) {
                agg.refundTxIds.add(row.getTransactionId());

                boolean hasRefundedItemsArray = false;
                if (row.getTransactionDetails() != null) {
                    try {
                        Map<String, Object> details = mapper.readValue(row.getTransactionDetails(), new TypeReference<Map<String, Object>>() {});
                        if (details.containsKey("refundedItems")) {
                            hasRefundedItemsArray = true;
                            List<Map<String, Object>> refundedItems = (List<Map<String, Object>>) details.get("refundedItems");
                            for (Map<String, Object> rItem : refundedItems) {
                                Number oId = (Number) rItem.get("orderItemId");
                                if (oId != null && oId.longValue() == row.getOrderItemId()) {
                                    Number amt = (Number) rItem.get("amount");
                                    if (amt != null) {
                                        agg.refundedRevenue += amt.doubleValue();
                                    }
                                    break; // Found the item, stop searching this array
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parse errors, fallback will occur if hasRefundedItemsArray remains false
                    }
                }

                if (!hasRefundedItemsArray) {
                    // M1 Legacy Schema (Full Refund Fallback)
                    agg.refundedRevenue += lineGross;
                }
            }
        }

        List<CategoryRevenueDTO> result = new ArrayList<>();
        for (CategoryAgg agg : map.values()) {
            double net = agg.grossRevenue - agg.refundedRevenue;
            long txCount = agg.txIds.size();
            long refCount = agg.refundTxIds.size();
            double rate = txCount == 0 ? 0.0 : ((double) refCount / txCount);

            result.add(new CategoryRevenueDTO.Builder()
                    .category(agg.category)
                    .grossRevenue(agg.grossRevenue)
                    .refundedRevenue(agg.refundedRevenue)
                    .netRevenue(net)
                    .transactionCount(txCount)
                    .refundCount(refCount)
                    .returnRate(rate)
                    .build());
        }
        return result;
    }
}
