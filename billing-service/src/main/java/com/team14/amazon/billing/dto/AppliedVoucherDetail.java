package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.DiscountType;
import java.time.LocalDateTime;

public class AppliedVoucherDetail {
    private String voucherCode;
    private DiscountType discountType;
    private Double discountApplied;
    private LocalDateTime appliedAt;

    public AppliedVoucherDetail(String voucherCode, DiscountType discountType,
                                Double discountApplied, LocalDateTime appliedAt) {
        this.voucherCode = voucherCode;
        this.discountType = discountType;
        this.discountApplied = discountApplied;
        this.appliedAt = appliedAt;
    }

    public String getVoucherCode() { return voucherCode; }
    public DiscountType getDiscountType() { return discountType; }
    public Double getDiscountApplied() { return discountApplied; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}