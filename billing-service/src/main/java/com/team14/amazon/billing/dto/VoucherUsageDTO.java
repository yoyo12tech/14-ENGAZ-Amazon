package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.DiscountType;

public record VoucherUsageDTO(
        Long voucherId,
        String code,
        DiscountType discountType,
        Double discountValue,
        Integer timesUsed,
        Double totalDiscountGiven,
        Boolean active,
        Boolean expired
) {}
