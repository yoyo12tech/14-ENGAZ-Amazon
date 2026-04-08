package com.team14.amazon.billing.controller;


import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.dto.UserTransactionSummaryDTO;
import com.team14.amazon.billing.dto.RevenueReportDTO;
import com.team14.amazon.billing.service.TransactionService;
import com.team14.amazon.billing.dto.ProcessTransactionRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/order/{orderId}")
    public ResponseEntity<Transaction> processOrderTransaction(
        @PathVariable Long orderId,
        @RequestBody ProcessTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.processOrderTransaction(orderId, request));
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
}
