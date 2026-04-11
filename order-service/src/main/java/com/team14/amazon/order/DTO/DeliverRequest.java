package com.team14.amazon.order.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class DeliverRequest {

    @NotNull(message = "Payment method is required")
    @Pattern(
            regexp = "CREDIT_CARD|CASH_ON_DELIVERY|WALLET",
            message = "Payment method must be one of: CREDIT_CARD, CASH_ON_DELIVERY, WALLET"
    )
    private String method;

    public DeliverRequest() {
    }

    public DeliverRequest(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
