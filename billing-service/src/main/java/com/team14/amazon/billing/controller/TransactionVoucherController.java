package com.team14.amazon.billing.controller;



import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.model.TransactionVoucher;
import com.team14.amazon.billing.service.TransactionVoucherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction-vouchers")
public class TransactionVoucherController {

    private final TransactionVoucherService transactionVoucherService;

    public TransactionVoucherController(TransactionVoucherService transactionVoucherService) {
        this.transactionVoucherService = transactionVoucherService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<TransactionVoucher> create(@RequestBody TransactionVoucher transactionVoucher) {
        TransactionVoucher savedVoucher = transactionVoucherService.create(transactionVoucher);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVoucher);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<TransactionVoucher>> getAll() {
        return ResponseEntity.ok(transactionVoucherService.getAll());
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<TransactionVoucher> getById(@PathVariable Long id) {
        return transactionVoucherService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<TransactionVoucher> update(
            @PathVariable Long id,
            @RequestBody TransactionVoucher transactionVoucher) {

        return transactionVoucherService.update(id, transactionVoucher)
                .map(updated -> {
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (transactionVoucherService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<TransactionVoucher>> getByTransactionId(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionVoucherService.getVouchersByTransactionId(transactionId));
    }

    @GetMapping("/top-used")
    public ResponseEntity<List<VoucherUsageDTO>> getTopUsedVouchers(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(transactionVoucherService.getTopUsedVouchers(limit));

    }
}