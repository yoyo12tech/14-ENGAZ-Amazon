package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.cache.BillingCacheInvalidationService;
import com.team14.amazon.billing.dto.*;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.service.PartialRefundService;
import com.team14.amazon.billing.service.TransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final BillingCacheInvalidationService cacheInvalidationService;
    private final PartialRefundService partialRefundService;

    public TransactionController(TransactionService transactionService,
                                 BillingCacheInvalidationService cacheInvalidationService,
                                 PartialRefundService partialRefundService) {
        this.transactionService = transactionService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.partialRefundService = partialRefundService;

    }

//    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NullPointerException.class})
//    public ResponseEntity<String> handleBadRequests(Exception e) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//    }

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        Transaction created = transactionService.createTransaction(transaction);
        cacheInvalidationService.evictTransactionCaches(created.getId());
        return created;
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
        Transaction updated = transactionService.updateTransaction(transaction);
        cacheInvalidationService.evictTransactionCaches(updated.getId());
        return updated;
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<Transaction> processRefund(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Requested by user";
        Transaction updated = transactionService.processRefund(id, reason);
        cacheInvalidationService.evictTransactionCaches(updated.getId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<Transaction> processOrderTransaction(
            @PathVariable Long orderId,
            @RequestParam(name = "simulateFailure", defaultValue = "false") boolean simulateFailure,
            @RequestBody ProcessTransactionRequest request) {
        Transaction updated = transactionService.processOrderTransaction(orderId, request, simulateFailure);
        cacheInvalidationService.evictTransactionCaches(updated.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(updated);
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<Transaction> retryTransaction(@PathVariable Long id) {
        Transaction updated = transactionService.retryTransaction(id);
        cacheInvalidationService.evictTransactionCaches(updated.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        cacheInvalidationService.evictTransactionCaches(id);
    }

    @GetMapping("/search")
    public List<Transaction> searchTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return transactionService.searchTransactions(status, startDate, endDate);
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

        Transaction updated = transactionService.applyVoucher(transactionId, voucherId);
        cacheInvalidationService.evictTransactionAndVoucherCaches(updated.getId(), voucherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }
    @PostMapping("/{id}/refund-items")
    public ResponseEntity<Transaction> processPartialRefund(
            @PathVariable Long id,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(partialRefundService.processPartialRefund(id, request));
    }
}
