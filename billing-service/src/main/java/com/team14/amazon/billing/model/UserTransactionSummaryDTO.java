package com.team14.amazon.billing.model;

import java.util.Map;

public record UserTransactionSummaryDTO(
    Long userId,
    Long totalTransactions,
    Double totalAmount,
    Map<String, Double> methodBreakdown
) {
}
