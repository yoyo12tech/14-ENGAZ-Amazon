package com.team14.amazon.billing.dto;

import java.util.Map;

public record UserTransactionSummaryDTO(
    Long userId,
    Long totalTransactions,
    Double totalAmount,
    Map<String, Double> methodBreakdown
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private Long totalTransactions;
        private Double totalAmount;
        private Map<String, Double> methodBreakdown;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder totalTransactions(Long totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public Builder totalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder methodBreakdown(Map<String, Double> methodBreakdown) {
            this.methodBreakdown = methodBreakdown;
            return this;
        }

        public UserTransactionSummaryDTO build() {
            return new UserTransactionSummaryDTO(userId, totalTransactions, totalAmount, methodBreakdown);
        }
    }
}
