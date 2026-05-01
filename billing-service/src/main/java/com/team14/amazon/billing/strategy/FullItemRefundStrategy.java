package com.team14.amazon.billing.strategy;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.model.Transaction;

import java.util.List;
import java.util.Map;

public class FullItemRefundStrategy implements RefundStrategy {

    private final List<Map<String, Object>> orderItems;

    public FullItemRefundStrategy(List<Map<String, Object>> orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public RefundResult calculateRefund(Transaction transaction, RefundRequest request) {
        List<Long> allItemIds = orderItems.stream()
                .map(item -> ((Number) item.get("id")).longValue())
                .toList();

        return new RefundResult(transaction.getAmount(), "full_refund", allItemIds);
    }
}