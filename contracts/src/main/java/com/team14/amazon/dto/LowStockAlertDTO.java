package com.team14.amazon.dto;

public record LowStockAlertDTO(
    Long productId,
    String productName,
    int currentStock,
    String category,
    long recentSalesCount,
    boolean hasNegativeReviews)
{}

