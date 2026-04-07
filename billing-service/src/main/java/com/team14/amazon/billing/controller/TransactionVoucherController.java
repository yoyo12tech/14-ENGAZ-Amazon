package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.service.TransactionService;
import com.team14.amazon.billing.service.VoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    // GET /api/transactions/voucher/top-used?limit={n}
    @GetMapping("/voucher/top-used")
    public List<VoucherUsageDTO> getTopUsedVouchers(@RequestParam int limit) {
        return voucherService.getTopUsedVouchers(limit);
    }
}