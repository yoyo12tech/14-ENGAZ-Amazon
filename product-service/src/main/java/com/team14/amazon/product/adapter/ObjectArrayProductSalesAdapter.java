package com.team14.amazon.product.adapter;

import com.team14.amazon.product.dto.ProductSalesDTO;

/**
 * Adapter pattern: adapts a native SQL Object[] row
 * from getProductSalesSummary() to ProductSalesDTO.
 */
public class ObjectArrayProductSalesAdapter {

    public ProductSalesDTO adapt(Object[] row) {
        return ProductSalesDTO.builder()
                .productId(((Number) row[0]).longValue())
                .name((String) row[1])
                .totalUnitsSold(((Number) row[2]).longValue())
                .totalRevenue(((Number) row[3]).doubleValue())
                .averageSellingPrice(((Number) row[4]).doubleValue())
                .build();
    }
}

