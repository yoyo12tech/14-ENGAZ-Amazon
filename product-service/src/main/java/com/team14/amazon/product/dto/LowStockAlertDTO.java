package com.team14.amazon.product.dto;

import com.team14.amazon.product.model.ProductCategory;

public class LowStockAlertDTO {



    private final Long productId;
    private final String productName;
    private final int currentStock;
    private final ProductCategory category;
    private final long recentSalesCount;
    private final boolean hasNegativeReviews;



    public LowStockAlertDTO(Long productId, String productName, int currentStock,
                            ProductCategory category, long recentSalesCount,
                            boolean hasNegativeReviews) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.category = category;
        this.recentSalesCount = recentSalesCount;
        this.hasNegativeReviews = hasNegativeReviews;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCurrentStock() { return currentStock; }
    public ProductCategory getCategory() { return category; }
    public long getRecentSalesCount() { return recentSalesCount; }
    public boolean isHasNegativeReviews() { return hasNegativeReviews; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long productId;
        private String productName;
        private int currentStock;
        private ProductCategory category;
        private long recentSalesCount;
        private boolean hasNegativeReviews;

        public Builder productId(Long value) { this.productId = value; return this; }
        public Builder productName(String value) { this.productName = value; return this; }
        public Builder currentStock(int value) { this.currentStock = value; return this; }
        public Builder category(ProductCategory value) { this.category = value; return this; }
        public Builder recentSalesCount(long value) { this.recentSalesCount = value; return this; }
        public Builder hasNegativeReviews(boolean value) { this.hasNegativeReviews = value; return this; }

        public LowStockAlertDTO build() {
            return new LowStockAlertDTO(productId, productName, currentStock, category, recentSalesCount, hasNegativeReviews);
        }
    }
}
