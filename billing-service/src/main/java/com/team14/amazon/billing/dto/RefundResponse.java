package com.team14.amazon.billing.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class RefundResponse {
    private Long refundId;
    private Long transactionId;
    private Double amount;
    private String reason;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public RefundResponse(Long refundId, Long transactionId, Double amount,
                          String reason, Map<String, Object> metadata, LocalDateTime createdAt) {
        this.refundId = refundId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.reason = reason;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public Long getRefundId() { return refundId; }
    public Long getTransactionId() { return transactionId; }
    public Double getAmount() { return amount; }
    public String getReason() { return reason; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}