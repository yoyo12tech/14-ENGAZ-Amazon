package com.team14.amazon.billing.repository;

public interface CategoryRevenueProjection {
    String getCategory();
    Long getOrderItemId();
    Integer getQuantity();
    Double getPriceAtPurchase();
    String getTransactionStatus();
    String getTransactionDetails(); // this is cast from jsonb to text
    Long getTransactionId();
}

