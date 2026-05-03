package com.team14.amazon.billing.strategy;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.model.Transaction;

import java.util.List;

public class NoRefundStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Transaction transaction, RefundRequest request) {
        return new RefundResult(0.0, "return_window_expired", List.of());
    }
}