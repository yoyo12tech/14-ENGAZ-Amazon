package com.team14.amazon.billing.dto;

public class CategoryRevenueDTO {
    private String category;
    private Double grossRevenue;
    private Double refundedRevenue;
    private Double netRevenue;
    private Long transactionCount;
    private Long refundCount;
    private Double returnRate;

    public CategoryRevenueDTO() {}

    private CategoryRevenueDTO(Builder builder) {
        this.category = builder.category;
        this.grossRevenue = builder.grossRevenue;
        this.refundedRevenue = builder.refundedRevenue;
        this.netRevenue = builder.netRevenue;
        this.transactionCount = builder.transactionCount;
        this.refundCount = builder.refundCount;
        this.returnRate = builder.returnRate;
    }

    public String getCategory() {
        return category;
    }

    public Double getGrossRevenue() {
        return grossRevenue;
    }

    public Double getRefundedRevenue() {
        return refundedRevenue;
    }

    public Double getNetRevenue() {
        return netRevenue;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public Long getRefundCount() {
        return refundCount;
    }

    public Double getReturnRate() {
        return returnRate;
    }

    public static class Builder {
        private String category;
        private Double grossRevenue;
        private Double refundedRevenue;
        private Double netRevenue;
        private Long transactionCount;
        private Long refundCount;
        private Double returnRate;

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder grossRevenue(Double grossRevenue) {
            this.grossRevenue = grossRevenue;
            return this;
        }

        public Builder refundedRevenue(Double refundedRevenue) {
            this.refundedRevenue = refundedRevenue;
            return this;
        }

        public Builder netRevenue(Double netRevenue) {
            this.netRevenue = netRevenue;
            return this;
        }

        public Builder transactionCount(Long transactionCount) {
            this.transactionCount = transactionCount;
            return this;
        }

        public Builder refundCount(Long refundCount) {
            this.refundCount = refundCount;
            return this;
        }

        public Builder returnRate(Double returnRate) {
            this.returnRate = returnRate;
            return this;
        }

        public CategoryRevenueDTO build() {
            return new CategoryRevenueDTO(this);
        }
    }
}

