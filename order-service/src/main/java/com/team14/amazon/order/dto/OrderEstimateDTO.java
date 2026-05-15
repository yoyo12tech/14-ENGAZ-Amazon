package com.team14.amazon.order.dto;

/**
 * DP-4 Builder Pattern — M1 DTO retrofitted with static inner Builder.
 * Used by M1 S3-F3  POST /api/orders/estimate.
 */
public class OrderEstimateDTO {

    private int    itemCount;
    private double subtotal;
    private double shippingCost;
    private double estimatedTotal;
    private double discountApplied;

    /** Required by Jackson for Redis deserialization. */
    public OrderEstimateDTO() {}

    /** Legacy all-args constructor — kept for backward compatibility. */
    public OrderEstimateDTO(int itemCount, double subtotal, double shippingCost,
                            double estimatedTotal, double discountApplied) {
        this.itemCount      = itemCount;
        this.subtotal       = subtotal;
        this.shippingCost   = shippingCost;
        this.estimatedTotal = estimatedTotal;
        this.discountApplied = discountApplied;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int    getItemCount()                        { return itemCount; }
    public void   setItemCount(int itemCount)           { this.itemCount = itemCount; }

    public double getSubtotal()                         { return subtotal; }
    public void   setSubtotal(double subtotal)          { this.subtotal = subtotal; }

    public double getShippingCost()                     { return shippingCost; }
    public void   setShippingCost(double shippingCost)  { this.shippingCost = shippingCost; }

    public double getEstimatedTotal()                   { return estimatedTotal; }
    public void   setEstimatedTotal(double v)           { this.estimatedTotal = v; }

    public double getDiscountApplied()                  { return discountApplied; }
    public void   setDiscountApplied(double v)          { this.discountApplied = v; }

    // ── DP-4: Builder ─────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final OrderEstimateDTO dto = new OrderEstimateDTO();

        public Builder itemCount(int v)        { dto.itemCount = v;       return this; }
        public Builder subtotal(double v)      { dto.subtotal = v;        return this; }
        public Builder shippingCost(double v)  { dto.shippingCost = v;    return this; }
        public Builder estimatedTotal(double v){ dto.estimatedTotal = v;  return this; }
        public Builder discountApplied(double v){ dto.discountApplied = v; return this; }

        public OrderEstimateDTO build()        { return dto; }
    }
}
