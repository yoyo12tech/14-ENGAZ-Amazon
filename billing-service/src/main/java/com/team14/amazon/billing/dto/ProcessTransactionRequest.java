package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.Method;

public class ProcessTransactionRequest {
    private Method method;
    private String cardLastFour;

    public Method getMethod() { return method; }
    public String getCardLastFour() { return cardLastFour; }

    public void setMethod(Method method) { this.method = method; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
}