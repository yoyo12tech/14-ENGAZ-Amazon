package com.team14.amazon.product.adapter;

import com.team14.amazon.product.dto.TopProductDTO;

/**
 * Adapter pattern: adapts a native SQL Object[] row
 * from findTopRatedProducts() to TopProductDTO.
 */
public class ObjectArrayTopProductAdapter {

    public TopProductDTO adapt(Object[] row) {
        return TopProductDTO.builder()
                .productId(((Number) row[0]).longValue())
                .name((String) row[1])
                .rating(((Number) row[2]).doubleValue())
                .totalSales(((Number) row[3]).longValue())
                .build();
    }
}

