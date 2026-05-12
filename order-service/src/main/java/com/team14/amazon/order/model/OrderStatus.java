
package com.team14.amazon.order.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    REFUNDED;

    public static OrderStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Order status cannot be null or empty");
        }

        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }
}