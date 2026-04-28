package com.team14.amazon.billing.dto;

public record RevenueReportDTO(
        Double totalRevenue,
        Long totalTransactions,
        Double averageTransaction,
        Double refundedAmount,
        Long refundCount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double totalRevenue;
        private Long totalTransactions;
        private Double averageTransaction;
        private Double refundedAmount;
        private Long refundCount;

        public Builder totalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder totalTransactions(Long totalTransactions) {
            this.totalTransactions = totalTransactions;
            return this;
        }

        public Builder averageTransaction(Double averageTransaction) {
            this.averageTransaction = averageTransaction;
            return this;
        }

        public Builder refundedAmount(Double refundedAmount) {
            this.refundedAmount = refundedAmount;
            return this;
        }

        public Builder refundCount(Long refundCount) {
            this.refundCount = refundCount;
            return this;
        }

        public RevenueReportDTO build() {
            return new RevenueReportDTO(totalRevenue, totalTransactions, averageTransaction, refundedAmount, refundCount);
        }
    }
}
