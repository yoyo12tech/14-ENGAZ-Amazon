package com.team14.amazon.billing.dto;

import java.util.Map;

public class RefundRequest {
    private Long transactionId;
    private String reason;
    private Map<String, Object> metadata;

    public Long getTransactionId() { return transactionId; }
    public String getReason() { return reason; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}