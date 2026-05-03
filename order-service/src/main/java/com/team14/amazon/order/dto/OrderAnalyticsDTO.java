package com.team14.amazon.order.dto;

/**
 * M1 DTO retrofitted with Builder pattern (DP-4).
 * Used by M1 S3-F6 GET /api/orders/analytics (unchanged endpoint).
 * Builder is required because this DTO has 6 fields (>= 5 threshold).
 */
public class OrderAnalyticsDTO {

    private long totalOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private double totalRevenue;
    private double averageOrderValue;
    private double completionRate;

    private OrderAnalyticsDTO() {}

    // Existing getters (unchanged for M1 compatibility)
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }

    public long getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(long deliveredOrders) { this.deliveredOrders = deliveredOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(double averageOrderValue) { this.averageOrderValue = averageOrderValue; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    /** DP-4: Builder entry point. */
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final OrderAnalyticsDTO dto = new OrderAnalyticsDTO();

        public Builder totalOrders(long v)        { dto.totalOrders = v; return this; }
        public Builder deliveredOrders(long v)    { dto.deliveredOrders = v; return this; }
        public Builder cancelledOrders(long v)    { dto.cancelledOrders = v; return this; }
        public Builder totalRevenue(double v)     { dto.totalRevenue = v; return this; }
        public Builder averageOrderValue(double v){ dto.averageOrderValue = v; return this; }
        public Builder completionRate(double v)   { dto.completionRate = v; return this; }

        public OrderAnalyticsDTO build() { return dto; }
    }

    /** Keep old all-args constructor for M1 backward compatibility. */
    public OrderAnalyticsDTO(long totalOrders, long deliveredOrders, long cancelledOrders,
                             double totalRevenue, double averageOrderValue, double completionRate) {
        this.totalOrders = totalOrders;
        this.deliveredOrders = deliveredOrders;
        this.cancelledOrders = cancelledOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.completionRate = completionRate;
    }
}
