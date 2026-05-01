package com.team14.amazon.order.dto;

/**
 * DP-4 Builder Pattern — new M2 DTO for S3-F12.
 * GET /api/orders/recommendations/{productId}
 *
 * Fields are enriched from PostgreSQL by the service layer —
 * Neo4j only provides productId and the co-purchase score.
 */
public class ProductRecommendationDTO {

    private Long   productId;
    private String name;
    private String category;
    private String brand;
    private Double price;
    private int    score;   // coPurchaseCount from the Neo4j edge

    public ProductRecommendationDTO() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long   getProductId()              { return productId; }
    public void   setProductId(Long v)        { this.productId = v; }

    public String getName()                   { return name; }
    public void   setName(String v)           { this.name = v; }

    public String getCategory()               { return category; }
    public void   setCategory(String v)       { this.category = v; }

    public String getBrand()                  { return brand; }
    public void   setBrand(String v)          { this.brand = v; }

    public Double getPrice()                  { return price; }
    public void   setPrice(Double v)          { this.price = v; }

    public int    getScore()                  { return score; }
    public void   setScore(int v)             { this.score = v; }

    // ── DP-4: Builder ─────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ProductRecommendationDTO dto = new ProductRecommendationDTO();

        public Builder productId(Long v)   { dto.productId = v;  return this; }
        public Builder name(String v)      { dto.name = v;       return this; }
        public Builder category(String v)  { dto.category = v;   return this; }
        public Builder brand(String v)     { dto.brand = v;      return this; }
        public Builder price(Double v)     { dto.price = v;      return this; }
        public Builder score(int v)        { dto.score = v;      return this; }

        public ProductRecommendationDTO build() { return dto; }
    }
}
