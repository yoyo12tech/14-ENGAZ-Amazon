package com.team14.amazon.product.dto;

public class TopProductDTO {

    private final Long productId;
    private final String name;
    private final Double rating;
    private final Long totalSales;


    public TopProductDTO(Long productId, String name, Double rating, Long totalSales) {
        this.productId = productId;
        this.name = name;
        this.rating = rating;
        this.totalSales = totalSales;
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public Double getRating() {
        return rating;
    }

    public Long getTotalSales() {
        return totalSales;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long productId;
        private String name;
        private Double rating;
        private Long totalSales;

        public Builder productId(Long value) { this.productId = value; return this; }
        public Builder name(String value) { this.name = value; return this; }
        public Builder rating(Double value) { this.rating = value; return this; }
        public Builder totalSales(Long value) { this.totalSales = value; return this; }

        public TopProductDTO build() {
            return new TopProductDTO(productId, name, rating, totalSales);
        }
    }
}

