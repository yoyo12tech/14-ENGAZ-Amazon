package com.team14.amazon.billing.dto;

public record RevenueReportDTO(
        Double totalRevenue,
        Long totalTransactions,
        Double averageTransaction,
        Double refundedAmount,
        Long refundCount
) {
}

