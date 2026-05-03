package com.team14.amazon.billing.strategy;

import java.util.List;

public class RefundResult {
    private final Double amount;
    private final String reasonCode;
    private final List<Long> refundedItemIds;

    public RefundResult(Double amount, String reasonCode, List<Long> refundedItemIds) {
        this.amount = amount;
        this.reasonCode = reasonCode;
        this.refundedItemIds = refundedItemIds;
    }

    public Double getAmount() { return amount; }
    public String getReasonCode() { return reasonCode; }
    public List<Long> getRefundedItemIds() { return refundedItemIds; }
}