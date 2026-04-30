package com.team14.amazon.order.dto;
import java.util.Map;

public record OrderItemRequestDTO(Long productId, Integer quantity, Map<String, Object> metadata) {}

