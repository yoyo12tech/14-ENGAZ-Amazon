package com.team14.amazon.billing.controller;


import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.dto.UserTransactionSummaryDTO;
import com.team14.amazon.billing.dto.RevenueReportDTO;
import com.team14.amazon.billing.service.TransactionService;

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
    public Transaction updateTransaction(Transaction transaction){
        return transactionService.updateTransaction(transaction);
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<Transaction> processRefund(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(transactionService.processRefund(id, body.get("reason")));
    }


    @DeleteMapping("/{id}")
    public void deleteTransaction(Long id){
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
//
//    @GetMapping

    @GetMapping("/reports/revenue")
    public RevenueReportDTO getRevenueReport(@RequestParam String startDate, @RequestParam String endDate) {
        return transactionService.getRevenueReport(startDate, endDate);
    }

    @PostMapping("/{transactionId}/voucher/{voucherId}")
    public ResponseEntity<Transaction> applyVoucher(
            @PathVariable Long transactionId,
            @PathVariable Long voucherId) {

        Transaction updated = transactionService.applyVoucher(transactionId, voucherId);
        return ResponseEntity.ok(updated);
    }
}
