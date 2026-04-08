package com.team14.amazon.product.dto;

public class TopProductDTO {
    private Long productId;
    private String name;
    private Double rating;
    private Long totalSales;

    public TopProductDTO(Long productId, String name, Double rating, Long totalSales) {
        this.productId = productId;
        this.name = name;
        this.rating = rating;
        this.totalSales = totalSales;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Long getTotalSales() { return totalSales; }
    public void setTotalSales(Long totalSales) { this.totalSales = totalSales; }
}