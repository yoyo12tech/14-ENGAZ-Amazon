package com.team14.amazon.dto;

public record OrderSummaryDTO(
    Long totalOrders,
    Long completedOrders,
    Long cancelledOrders,
    Double totalSpent,
    Double averageOrderValue
){}