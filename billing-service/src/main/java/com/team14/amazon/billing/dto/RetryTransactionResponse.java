package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.Status;

public class RetryTransactionResponse {
    private Long transactionId;
    private Status previousStatus;
    private Status newStatus;
    private String message;

    public RetryTransactionResponse(Long transactionId, Status previousStatus,
                                    Status newStatus, String message) {
        this.transactionId = transactionId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.message = message;
    }

    public Long getTransactionId() { return transactionId; }
    public Status getPreviousStatus() { return previousStatus; }
    public Status getNewStatus() { return newStatus; }
    public String getMessage() { return message; }
}