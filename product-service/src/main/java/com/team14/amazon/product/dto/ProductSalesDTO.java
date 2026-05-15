package com.team14.amazon.product.dto;

public class ProductSalesDTO {

    private Long productId;
    private String name;
    private Long totalUnitsSold;
    private Double totalRevenue;
    private Double averageSellingPrice;

    public ProductSalesDTO() {
    }
    public ProductSalesDTO(Long productId, String name, Long totalUnitsSold,
                           Double totalRevenue, Double averageSellingPrice) {
        this.productId = productId;
        this.name = name;
        this.totalUnitsSold = totalUnitsSold;
        this.totalRevenue = totalRevenue;
        this.averageSellingPrice = averageSellingPrice;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getTotalUnitsSold() { return totalUnitsSold; }
    public void setTotalUnitsSold(Long totalUnitsSold) { this.totalUnitsSold = totalUnitsSold; }
    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    public Double getAverageSellingPrice() { return averageSellingPrice; }
    public void setAverageSellingPrice(Double averageSellingPrice) { this.averageSellingPrice = averageSellingPrice; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long productId;
        private String name;
        private Long totalUnitsSold;
        private Double totalRevenue;
        private Double averageSellingPrice;

        public Builder productId(Long value) { this.productId = value; return this; }
        public Builder name(String value) { this.name = value; return this; }
        public Builder totalUnitsSold(Long value) { this.totalUnitsSold = value; return this; }
        public Builder totalRevenue(Double value) { this.totalRevenue = value; return this; }
        public Builder averageSellingPrice(Double value) { this.averageSellingPrice = value; return this; }

        public ProductSalesDTO build() {
            return new ProductSalesDTO(productId, name, totalUnitsSold, totalRevenue, averageSellingPrice);
        }
    }
}