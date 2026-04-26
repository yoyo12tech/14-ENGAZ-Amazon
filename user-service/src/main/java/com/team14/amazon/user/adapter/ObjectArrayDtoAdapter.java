package com.team14.amazon.user.adapter;

import com.team14.amazon.user.dto.UserOrderSummaryDTO;
import com.team14.amazon.user.model.User;

public class ObjectArrayDtoAdapter {

    public static UserOrderSummaryDTO adaptToUserOrderSummaryDTO(User user, Object[] row) {
        long totalOrders     = row[0] != null ? ((Number) row[0]).longValue()   : 0L;
        long completedOrders = row[1] != null ? ((Number) row[1]).longValue()   : 0L;
        long cancelledOrders = row[2] != null ? ((Number) row[2]).longValue()   : 0L;
        double totalSpent    = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        double avgOrderValue = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

        avgOrderValue = Math.round(avgOrderValue * 100.0) / 100.0;

        return UserOrderSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalSpent(totalSpent)
                .averageOrderValue(avgOrderValue)
                .build();
    }

    public static com.team14.amazon.user.dto.TopBuyerDTO adaptToTopBuyerDTO(Object[] row) {
        return com.team14.amazon.user.dto.TopBuyerDTO.builder()
                .userId(((Number) row[0]).longValue())
                .name((String) row[1])
                .totalSpent(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                .orderCount(((Number) row[3]).longValue())
                .build();
    }
}
