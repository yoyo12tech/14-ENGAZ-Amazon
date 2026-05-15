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
import com.team14.amazon.contracts.feign.OrderServiceClient;
import com.team14.amazon.contracts.feign.ProductServiceClient;
import com.team14.amazon.contracts.feign.ShippingServiceClient;
import com.team14.amazon.contracts.feign.UserServiceClient;
import com.team14.amazon.contracts.dto.OrderDTO;
import com.team14.amazon.billing.messaging.PaymentEventPublisher;
import com.team14.amazon.contracts.events.PaymentCompletedEvent;
import com.team14.amazon.contracts.events.PaymentFailedEvent;

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
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.team14.amazon.billing.dto.AppliedVoucherDetail;
import com.team14.amazon.billing.dto.TransactionDetailsDTO;
import com.team14.amazon.billing.dto.CategoryRevenueDTO;
import com.team14.amazon.billing.dto.LifecycleEventDTO;
import com.team14.amazon.billing.exception.ConflictException;
import com.team14.amazon.contracts.dto.OrderDetailsDTO;
import com.team14.amazon.contracts.dto.OrderItemDTO;
import com.team14.amazon.contracts.dto.ProductDTO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.bson.Document;
import java.util.Comparator;
import java.time.ZoneId;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionVoucherRepository transactionVoucherRepository;
    private final List<EntityObserver> observers;
    private final BillingCacheInvalidationService cacheInvalidationService;
    private final MongoTemplate mongoTemplate;

    private final OrderServiceClient orderServiceClient;
    private final ProductServiceClient productServiceClient;
    private final ShippingServiceClient shippingServiceClient;
    private final UserServiceClient userServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;

    public TransactionService(TransactionRepository transactionRepository, VoucherRepository voucherRepository,
            TransactionVoucherRepository transactionVoucherRepository,
            List<EntityObserver> observers, BillingCacheInvalidationService cacheInvalidationService,
            MongoTemplate mongoTemplate,
            OrderServiceClient orderServiceClient,
            ProductServiceClient productServiceClient,
            ShippingServiceClient shippingServiceClient,
            UserServiceClient userServiceClient,
            PaymentEventPublisher paymentEventPublisher) {
        this.transactionRepository = transactionRepository;
        this.voucherRepository = voucherRepository;
        this.transactionVoucherRepository = transactionVoucherRepository;
        this.observers = new ArrayList<>(observers);
        this.cacheInvalidationService = cacheInvalidationService;
        this.mongoTemplate = mongoTemplate;
        this.orderServiceClient = orderServiceClient;
        this.productServiceClient = productServiceClient;
        this.shippingServiceClient = shippingServiceClient;
        this.userServiceClient = userServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
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

    public Transaction findTransactionByIdDirect(Long id) {
        return transactionRepository.findById(id).orElse(null);
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

    public java.math.BigDecimal getUserTransactionTotal(Long userId, String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        return transactionRepository.getUserTransactionTotal(userId, start, end);
    }

    public long getUserOrderCount(Long userId, String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        return transactionRepository.getUserOrderCount(userId, start, end);
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
        com.team14.amazon.contracts.dto.UserDTO user = userServiceClient.getUser(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(userId) && t.getStatus() == TransactionStatus.COMPLETED)
                .toList();

        Map<String, Double> methodBreakdown = new HashMap<>();

        long totalTransactions = 0;
        double totalVolume = 0.0;

        for (Transaction t : transactions) {
            String mStr = t.getMethod() != null ? t.getMethod().name() : "UNKNOWN";
            double amt = t.getAmount() != null ? t.getAmount() : 0.0;

            methodBreakdown.put(mStr, methodBreakdown.getOrDefault(mStr, 0.0) + amt);

            totalTransactions++;
            totalVolume += amt;
        }

        return UserTransactionSummaryDTO.builder()
                .userId(userId)
                .totalTransactions(totalTransactions)
                .totalAmount(totalVolume)
                .methodBreakdown(methodBreakdown)
                .build();
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

        ProcessTransactionRequest req = request != null ? request : new ProcessTransactionRequest();

        OrderDTO order;
        try {
            order = orderServiceClient.getOrder(orderId);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
            }
            throw e;
        }

        String oStatus = order != null ? order.status() : "";

        if (!"DELIVERED".equalsIgnoreCase(oStatus) && !"PAYMENT_PENDING".equalsIgnoreCase(oStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }

        Transaction existing = transactionRepository.findByOrderId(orderId);
        if (existing == null || existing.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not initialized for this order - saga state may be corrupt; check billing-service logs");
        }

        TransactionMethod effectiveMethod = existing.getMethod();
        if (req.getMethod() != null) {
            effectiveMethod = req.getMethod();
        }

        existing.setMethod(effectiveMethod);

        if (simulateFailure) {
            int updated = transactionRepository.updateStatusAtomically(existing.getId(), TransactionStatus.PENDING, TransactionStatus.FAILED);
            if (updated == 0) {
                throw new ConflictException("Conflict updating status");
            }
            existing.setStatus(TransactionStatus.FAILED);
            Map<String, Object> details = new HashMap<>();
            if (existing.getTransactionDetails() != null) {
                details.putAll(existing.getTransactionDetails());
            }
            details.put("simulateFailure", true);
            existing.setTransactionDetails(details);
            Transaction saved = transactionRepository.save(existing);

            paymentEventPublisher.publishFailed(new PaymentFailedEvent(saved.getId(), orderId, "SIMULATED_FAILURE"));
            cacheInvalidationService.evictTransactionCaches(saved.getId());
            return saved;
        }

        int updated = transactionRepository.updateStatusAtomically(existing.getId(), TransactionStatus.PENDING, TransactionStatus.COMPLETED);
        if (updated == 0) {
            throw new ConflictException("Conflict updating status");
        }

        existing.setStatus(TransactionStatus.COMPLETED);
        Map<String, Object> details = new HashMap<>();
        if (existing.getTransactionDetails() != null) {
            details.putAll(existing.getTransactionDetails());
        }
        details.put("gatewayResponse", "approved");
        if (req.getCardLastFour() != null) {
            details.put("cardLastFour", req.getCardLastFour());
        }
        existing.setTransactionDetails(details);

        Transaction saved = transactionRepository.save(existing);

        paymentEventPublisher.publishCompleted(new PaymentCompletedEvent(saved.getId(), orderId, saved.getUserId(), saved.getAmount()));
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

        List<Transaction> transactions = transactionRepository.findByCreatedAtBetweenAndStatusIn(start, end,
                List.of(TransactionStatus.COMPLETED, TransactionStatus.REFUNDED));

        Map<Long, List<OrderItemDTO>> orderItemsCache = new HashMap<>();
        for (Transaction t : transactions) {
            Long oid = t.getOrderId();
            if (oid != null) {
                orderItemsCache.computeIfAbsent(oid, this::loadOrderItemsForOrder);
            }
        }

        Map<Long, String> productCategoryCache = new HashMap<>();
        Set<Long> allProductIds = new HashSet<>();
        for (List<OrderItemDTO> items : orderItemsCache.values()) {
            for (OrderItemDTO oi : items) {
                if (oi.productId() != null) {
                    allProductIds.add(oi.productId());
                }
            }
        }

        List<Long> idList = new ArrayList<>(allProductIds);
        final int batchSize = 80;
        for (int i = 0; i < idList.size(); i += batchSize) {
            List<Long> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
            List<ProductDTO> products = productServiceClient.getProductsBatch(batch);
            for (ProductDTO p : products) {
                if (p.id() != null) {
                    productCategoryCache.put(p.id(), p.category() != null ? p.category() : "UNKNOWN");
                }
            }
        }

        class CategoryAgg {
            String category;
            double grossRevenue = 0.0;
            double refundedRevenue = 0.0;
            Set<Long> txIds = new HashSet<>();
            Set<Long> refundTxIds = new HashSet<>();

            CategoryAgg(String c) {
                this.category = c;
            }
        }

        Map<String, CategoryAgg> map = new HashMap<>();

        for (Transaction t : transactions) {
            Long oid = t.getOrderId();
            if (oid == null) {
                continue;
            }
            List<OrderItemDTO> items = orderItemsCache.getOrDefault(oid, List.of());
            for (OrderItemDTO oi : items) {
                if (oi.id() == null || oi.quantity() == null || oi.priceAtPurchase() == null) {
                    continue;
                }
                Long pid = oi.productId();
                String cat = pid == null ? "UNKNOWN" : productCategoryCache.getOrDefault(pid, "UNKNOWN");
                map.putIfAbsent(cat, new CategoryAgg(cat));
                CategoryAgg agg = map.get(cat);

                agg.txIds.add(t.getId());
                double lineGross = oi.quantity() * oi.priceAtPurchase();
                agg.grossRevenue += lineGross;

                if (t.getStatus() == TransactionStatus.REFUNDED) {
                    agg.refundTxIds.add(t.getId());

                    boolean hasRefundedItemsArray = false;
                    Map<String, Object> det = t.getTransactionDetails();
                    if (det != null && det.containsKey("refundedItems")) {
                        Object rawRi = det.get("refundedItems");
                        if (rawRi instanceof List<?> refundedItems) {
                            hasRefundedItemsArray = true;
                            for (Object o : refundedItems) {
                                if (!(o instanceof Map<?, ?> m)) {
                                    continue;
                                }
                                Object oIdObj = m.get("orderItemId");
                                if (!(oIdObj instanceof Number num)) {
                                    continue;
                                }
                                if (num.longValue() != oi.id()) {
                                    continue;
                                }
                                Object amtObj = m.get("amount");
                                if (amtObj instanceof Number amt) {
                                    agg.refundedRevenue += amt.doubleValue();
                                }
                                break;
                            }
                        }
                    }

                    if (!hasRefundedItemsArray) {
                        agg.refundedRevenue += lineGross;
                    }
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

    @Cacheable(cacheNames = BillingCacheNames.S5_F11, key = "#transactionId")
    public List<LifecycleEventDTO> getTransactionLifecycle(Long transactionId) {
//        if (auth == null || !auth.isAuthenticated()) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
//        }
//
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));


//        String uidStr = (String) auth.getPrincipal();
//        Long uid;
//        try {
//            uid = Long.parseLong(uidStr);
//        } catch (NumberFormatException e) {
//             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user ID in token");
//        }
//
//        boolean isAdmin = auth.getAuthorities().stream()
//                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
//
//        if (!transaction.getUserId().equals(uid) && !isAdmin) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: You do not own this transaction");
//        }

        List<Long> shipmentIds = new ArrayList<>();
        if (transaction.getOrderId() != null) {
            try {
                List<Long> ids = shippingServiceClient.getShipmentIdsForOrder(transaction.getOrderId());
                if (ids != null) {
                    shipmentIds = ids;
                }
            } catch (ResponseStatusException e) {
                if (e.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                    throw e;
                }
            }
        }
        List<LifecycleEventDTO> timeline = new ArrayList<>();

        // 1. Transaction Events
        Query tQuery = new Query(Criteria.where("transactionId").is(transactionId));
        List<Document> tDocs = mongoTemplate.find(tQuery, Document.class, "transaction_audit_trail");
        for (Document d : tDocs) {
            timeline.add(mapToDTO(d, "TRANSACTION"));
        }

        // 2. Order Events
        if (transaction.getOrderId() != null) {
            Query oQuery = new Query(Criteria.where("orderId").is(transaction.getOrderId()));
            List<Document> oDocs = mongoTemplate.find(oQuery, Document.class, "order_events");
            for (Document d : oDocs) {
                timeline.add(mapToDTO(d, "ORDER"));
            }
        }

        // 3. Shipment Events
        if (!shipmentIds.isEmpty()) {
            Query sQuery = new Query(Criteria.where("shipmentId").in(shipmentIds));
            List<Document> sDocs = mongoTemplate.find(sQuery, Document.class, "shipment_events");
            for (Document d : sDocs) {
                timeline.add(mapToDTO(d, "SHIPMENT"));
            }
        }

        timeline.sort(Comparator.comparing(LifecycleEventDTO::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    private List<OrderItemDTO> loadOrderItemsForOrder(Long orderId) {
        try {
            OrderDetailsDTO details = orderServiceClient.getOrderDetails(orderId);
            if (details != null && details.getItems() != null) {
                return details.getItems();
            }
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                throw e;
            }
        }
        return List.of();
    }

    private LifecycleEventDTO mapToDTO(Document d, String source) {
        LocalDateTime timestamp = null;
        Object tsObj = d.get("timestamp");
        if (tsObj instanceof java.util.Date) {
            timestamp = ((java.util.Date) tsObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (tsObj instanceof String) {
            String tsStr = (String) tsObj;
            try {
                timestamp = LocalDateTime.parse(tsStr);
            } catch (Exception e) {
                try {
                    timestamp = LocalDateTime.ofInstant(Instant.parse(tsStr), ZoneId.systemDefault());
                } catch (Exception ex) {
                    // Fallback if parsing fails
                }
            }
        } else if (tsObj instanceof LocalDateTime) {
            timestamp = (LocalDateTime) tsObj;
        } else if (tsObj instanceof Long) {
            timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) tsObj), ZoneId.systemDefault());
        } else if (tsObj instanceof Integer) {
            timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Integer) tsObj).longValue()), ZoneId.systemDefault());
        }
        
        // details can be a nested Document or a Map — copy to plain HashMap so Jackson can serialize/deserialize it cleanly from Redis cache
        Map<String, Object> details = null;
        Object detailsObj = d.get("details");
        if (detailsObj instanceof Map) {
            details = new HashMap<>((Map<String, Object>) detailsObj);
        }

        return LifecycleEventDTO.builder()
                .timestamp(timestamp)
                .source(source)
                .action(d.getString("action"))
                .details(details)
                .build();
    }
}
