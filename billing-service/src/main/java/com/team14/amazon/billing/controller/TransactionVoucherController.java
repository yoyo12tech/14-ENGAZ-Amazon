package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.dto.TransactionDetailsDTO;
import com.team14.amazon.billing.service.TransactionService;
import com.team14.amazon.billing.service.VoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.service.TransactionService;
import com.team14.amazon.billing.service.VoucherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionVoucherController {
    private final TransactionService transactionService;
    private final VoucherService voucherService;

    public TransactionVoucherController(TransactionService transactionService,
                                        VoucherService voucherService) {
        this.transactionService = transactionService;
        this.voucherService = voucherService;
    }
    // GET /api/transactions/{transactionId}/details
    @GetMapping("/{transactionId}/details")
    public TransactionDetailsDTO getTransactionDetails(@PathVariable Long transactionId) {
        return transactionService.getTransactionDetails(transactionId);}
        
    @PostMapping("/{transactionId}/voucher/{voucherId}")
    public Transaction applyVoucher(@PathVariable Long transactionId,
                                    @PathVariable Long voucherId) {
        return transactionService.applyVoucherToTransaction(transactionId, voucherId);
    }
}