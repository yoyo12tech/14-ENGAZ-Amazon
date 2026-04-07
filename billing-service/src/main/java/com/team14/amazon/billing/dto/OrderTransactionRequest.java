package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.Method;

public class OrderTransactionRequest {
    private Long orderId;
    private Long userId;
    private Double amount;
    private Method method;

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Double getAmount() { return amount; }
    public Method getMethod() { return method; }

    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setMethod(Method method) { this.method = method; }
}