package com.team14.amazon.billing.model.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "transaction_audit_trail")
public class TransactionAuditEvent implements MongoEvent {

    @Id
    private String id;
    private Long transactionId;
    private String action;
    private LocalDateTime timestamp;
    private String method;
    private Double amount;
    private Map<String, Object> details;

    public TransactionAuditEvent() {
    }

    public TransactionAuditEvent(String id, Long transactionId, String action, LocalDateTime timestamp,
            String method, Double amount, Map<String, Object> details) {
        this.id = id;
        this.transactionId = transactionId;
        this.action = action;
        this.timestamp = timestamp;
        this.method = method;
        this.amount = amount;
        this.details = details;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
