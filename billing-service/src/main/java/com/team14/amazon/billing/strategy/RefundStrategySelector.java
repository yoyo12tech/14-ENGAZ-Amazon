package com.team14.amazon.billing.strategy;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RefundStrategySelector {

    private static final int RETURN_WINDOW_DAYS = 30;

    private final List<Map<String, Object>> orderItems;

    public RefundStrategySelector(List<Map<String, Object>> orderItems) {
        this.orderItems = orderItems;
    }

    public RefundStrategy select(Transaction transaction, RefundRequest request) {
        boolean withinWindow = transaction.getCreatedAt()
                .isAfter(LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS));

        if (!withinWindow) {
            return new NoRefundStrategy();
        }

        if (Boolean.TRUE.equals(request.getRefundAll())) {
            return new FullItemRefundStrategy(orderItems);
        }

        return new PartialItemRefundStrategy(orderItems);
    }
}