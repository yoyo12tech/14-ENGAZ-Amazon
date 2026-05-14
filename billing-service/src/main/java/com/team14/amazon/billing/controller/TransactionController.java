package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.dto.*;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.service.PartialRefundService;
import com.team14.amazon.billing.service.TransactionService;
import com.team14.amazon.billing.observer.MongoEventLogger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final PartialRefundService partialRefundService;
    private final MongoEventLogger mongoEventLogger;

    public TransactionController(TransactionService transactionService,
                                 PartialRefundService partialRefundService,
                                 MongoEventLogger mongoEventLogger) {
        this.transactionService = transactionService;
        this.partialRefundService = partialRefundService;
        this.mongoEventLogger = mongoEventLogger;
    }

//    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NullPointerException.class})
//    public ResponseEntity<String> handleBadRequests(Exception e) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//    }

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        return transactionService.createTransaction(transaction);
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id) {
        return transactionService.findTransactionById(id);
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.findAllTransactions();
    }

    @PutMapping("/{id}")
    public Transaction updateTransaction(@PathVariable Long id, @RequestBody Transaction transaction) {
        transaction.setId(id);
        return transactionService.updateTransaction(transaction);
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<Transaction> processRefund(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Requested by user";
        return ResponseEntity.ok(transactionService.processRefund(id, reason));
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<Transaction> processOrderTransaction(
            @PathVariable Long orderId,
            @RequestParam(name = "simulateFailure", defaultValue = "false") boolean simulateFailure,
            @RequestBody ProcessTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.processOrderTransaction(orderId, request, simulateFailure));
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<Transaction> retryTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.retryTransaction(id));
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }

    @GetMapping("/search")
    public List<Transaction> searchTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return transactionService.searchTransactions(status, startDate, endDate);
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<BigDecimal> getUserTransactionTotal(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(transactionService.getUserTransactionTotal(userId, startDate, endDate));
    }

    @GetMapping("/user/{userId}/order-count")
    public ResponseEntity<Long> getUserOrderCount(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(transactionService.getUserOrderCount(userId, startDate, endDate));
    }

    @GetMapping("/user/{userId}/summary")
    public UserTransactionSummaryDTO getUserTransactionSummary(@PathVariable Long userId) {
        return transactionService.getUserTransactionSummary(userId);
    }

    @GetMapping("/{transactionId}/details")
    public ResponseEntity<TransactionDetailsDTO> getTransactionDetails(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionDetails(transactionId));
    }

    @GetMapping("/reports/revenue")
    public RevenueReportDTO getRevenueReport(@RequestParam String startDate, @RequestParam String endDate) {
        return transactionService.getRevenueReport(startDate, endDate);
    }

    @GetMapping("/voucher/top-used")
    public ResponseEntity<List<VoucherUsageDTO>> getTopUsedVouchers(
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(transactionService.getTopUsedVouchers(limit));
    }

    @PostMapping("/{transactionId}/voucher/{voucherId}")
    public ResponseEntity<Transaction> applyVoucher(
            @PathVariable Long transactionId,
            @PathVariable Long voucherId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.applyVoucher(transactionId, voucherId));
    }
    @PostMapping("/{id}/refund-items")
    public ResponseEntity<Transaction> processPartialRefund(
            @PathVariable Long id,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(partialRefundService.processPartialRefund(id, request));
    }

    @GetMapping("/analytics/category")
    public ResponseEntity<List<CategoryRevenueDTO>> getCategoryRevenue(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> logPayload = new HashMap<>();
        logPayload.put("report", "CATEGORY_REVENUE");
        logPayload.put("startDate", startDate.toString());
        logPayload.put("endDate", endDate.toString());
        mongoEventLogger.onEvent("ANALYTICS_VIEWED", logPayload);

        return ResponseEntity.ok(transactionService.getCategoryRevenue(startDate, endDate));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/lifecycle")
    public ResponseEntity<List<LifecycleEventDTO>> getTransactionLifecycle(@PathVariable Long id, Authentication auth) {
        Transaction transaction = transactionService.findTransactionByIdDirect(id);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        Long uid = -1L;
        try {
            uid = Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            // ignore, uid remains -1L
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!uid.equals(transaction.getUserId()) && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(transactionService.getTransactionLifecycle(id));
    }
}
