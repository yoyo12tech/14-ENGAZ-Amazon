package com.team14.amazon.product.dto;

import com.team14.amazon.product.model.ProductCategory;

public record LowStockAlertDTO(
        Long   productId,
        String productName,
        int    currentStock,
        ProductCategory category,
        long   recentSalesCount,
        boolean hasNegativeReviews
) {}