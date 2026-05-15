package com.team14.amazon.contracts.dto;

public record OrderItemDTO(
        Long id,
        Long productId,
        Integer quantity,
        Double priceAtPurchase,
        Integer itemOrder
) {}