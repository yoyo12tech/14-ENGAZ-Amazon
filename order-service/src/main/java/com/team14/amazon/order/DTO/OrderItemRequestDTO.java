package com.team14.amazon.order.DTO;
import java.util.Map;

public record OrderItemRequestDTO(Long productId, Integer quantity, Map<String, Object> metadata) {}

