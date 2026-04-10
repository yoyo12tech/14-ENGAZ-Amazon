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
}

