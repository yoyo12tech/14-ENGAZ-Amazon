package com.team14.amazon.billing.controller;

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
    @PostMapping("/{transactionId}/voucher/{voucherId}")
    public Transaction applyVoucher(@PathVariable Long transactionId,
                                    @PathVariable Long voucherId) {
        return transactionService.applyVoucherToTransaction(transactionId, voucherId);
    }
}