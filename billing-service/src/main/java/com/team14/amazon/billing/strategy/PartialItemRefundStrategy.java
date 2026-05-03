package com.team14.amazon.billing.strategy;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.model.Transaction;

import java.util.List;
import java.util.Map;

public class PartialItemRefundStrategy implements RefundStrategy {

    private final List<Map<String, Object>> orderItems;

    public PartialItemRefundStrategy(List<Map<String, Object>> orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public RefundResult calculateRefund(Transaction transaction, RefundRequest request) {
        List<Long> requestedIds = request.getOrderItemIds();

        double refundAmount = orderItems.stream()
                .filter(item -> requestedIds.contains(((Number) item.get("id")).longValue()))
                .mapToDouble(item -> {
                    double price = ((Number) item.get("priceAtPurchase")).doubleValue();
                    int qty = ((Number) item.get("quantity")).intValue();
                    return price * qty;
                })
                .sum();

        return new RefundResult(refundAmount, "partial_refund", requestedIds);
    }
}