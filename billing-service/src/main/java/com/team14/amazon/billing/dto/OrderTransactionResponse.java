package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import java.time.LocalDateTime;

public class OrderTransactionResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private Double amount;
    private Method method;
    private Status status;
    private LocalDateTime createdAt;

    public OrderTransactionResponse(Long id, Long orderId, Long userId, Double amount,
                                    Method method, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Double getAmount() { return amount; }
    public Method getMethod() { return method; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}