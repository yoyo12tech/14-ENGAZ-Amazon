package com.team14.amazon.order.dto;
import com.team14.amazon.order.model.OrderStatus;
import java.util.List;
import java.util.Map;


public record OrderDetailsDTO(
        Long orderId, Long userId, Long shippingAddressId, OrderStatus status,
        Double totalAmount, Map<String, Object> metadata, List<OrderItemDTO> items,
        int totalItems, int totalQuantity) {}