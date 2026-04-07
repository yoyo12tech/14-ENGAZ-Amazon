package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.dto.RefundResponse;
import com.team14.amazon.billing.service.RefundService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refund")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<RefundResponse> processRefund(@RequestBody RefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.processRefund(request));
    }
}