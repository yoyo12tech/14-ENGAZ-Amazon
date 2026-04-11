package com.team14.amazon.billing.controller;


import com.team14.amazon.billing.dto.*;
import com.team14.amazon.billing.model.Transaction;
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

    public TransactionController(TransactionService transactionService){
        this.transactionService = transactionService;
    }

//    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NullPointerException.class})
//    public ResponseEntity<String> handleBadRequests(Exception e) {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//    }

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction){
        return transactionService.createTransaction(transaction);
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id){
        return transactionService.findTransactionById(id);
    }

    @GetMapping
    public List<Transaction> getAllTransactions(){
        return transactionService.findAllTransactions();
    }

    @PutMapping("/{id}")
    public Transaction updateTransaction(@PathVariable Long id, @RequestBody Transaction transaction) {
        transaction.setId(id); // Ensure the ID matches the path
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
            @RequestBody ProcessTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.processOrderTransaction(orderId, request));
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

    @GetMapping("/user/{userId}/summary")
    public UserTransactionSummaryDTO getUserTransactionSummary(@PathVariable Long userId) {
        return transactionService.getUserTransactionSummary(userId);
    }

//    @GetMapping
    @GetMapping("/{transactionId}/details")
    public ResponseEntity<TransactionDetailsDTO> getTransactionDetails(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionDetails(transactionId));
    }
//
//    @GetMapping

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
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }
}
