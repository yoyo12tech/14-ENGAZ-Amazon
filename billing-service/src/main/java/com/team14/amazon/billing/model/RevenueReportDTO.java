package com.team14.amazon.billing.model;

public record RevenueReportDTO(
        Double totalRevenue,
        Long totalTransactions,
        Double averageTransaction,
        Double refundedAmount,
        Long refundCount
) {
}

