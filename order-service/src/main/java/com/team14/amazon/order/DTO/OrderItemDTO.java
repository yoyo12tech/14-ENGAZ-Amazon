package com.team14.amazon.order.DTO;
import java.util.Map;
public record OrderItemDTO(Long id, Integer itemOrder, Long productId, Integer quantity, Double priceAtPurchase, Map<String, Object> metadata) {}