package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.TransactionMethod;

public class ProcessTransactionRequest {
    private TransactionMethod method;
    private String cardLastFour;

    public TransactionMethod getMethod() { return method; }
    public String getCardLastFour() { return cardLastFour; }

    public void setMethod(TransactionMethod method) { this.method = method; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
}