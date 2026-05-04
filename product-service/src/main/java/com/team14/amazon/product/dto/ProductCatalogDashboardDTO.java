package com.team14.amazon.product.dto;

import java.util.Map;

/**
 * S2-F12 Product Catalog Dashboard DTO.
 * Builder pattern required (M2 §3.5 — dashboard DTO with 6 fields).
 */
public class ProductCatalogDashboardDTO {

    private long totalProducts;
    private long outOfStockCount;
    private double averageRating;
    private Map<String, Long> categoryDistribution;
    private double averagePrice;
    private long lowStockCount;

    private ProductCatalogDashboardDTO() {}

    public long getTotalProducts() { return totalProducts; }
    public long getOutOfStockCount() { return outOfStockCount; }
    public double getAverageRating() { return averageRating; }
    public Map<String, Long> getCategoryDistribution() { return categoryDistribution; }
    public double getAveragePrice() { return averagePrice; }
    public long getLowStockCount() { return lowStockCount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalProducts;
        private long outOfStockCount;
        private double averageRating;
        private Map<String, Long> categoryDistribution;
        private double averagePrice;
        private long lowStockCount;

        public Builder totalProducts(long v) { this.totalProducts = v; return this; }
        public Builder outOfStockCount(long v) { this.outOfStockCount = v; return this; }
        public Builder averageRating(double v) { this.averageRating = v; return this; }
        public Builder categoryDistribution(Map<String, Long> v) { this.categoryDistribution = v; return this; }
        public Builder averagePrice(double v) { this.averagePrice = v; return this; }
        public Builder lowStockCount(long v) { this.lowStockCount = v; return this; }

        public ProductCatalogDashboardDTO build() {
            ProductCatalogDashboardDTO dto = new ProductCatalogDashboardDTO();
            dto.totalProducts = this.totalProducts;
            dto.outOfStockCount = this.outOfStockCount;
            dto.averageRating = this.averageRating;
            dto.categoryDistribution = this.categoryDistribution;
            dto.averagePrice = this.averagePrice;
            dto.lowStockCount = this.lowStockCount;
            return dto;
        }
    }
}

