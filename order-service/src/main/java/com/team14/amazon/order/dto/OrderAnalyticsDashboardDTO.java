package com.team14.amazon.order.dto;

import java.util.Map;

/**
 * DP-4 Builder Pattern — new M2 DTO for S3-F10.
 * GET /api/orders/analytics/dashboard
 *
 * Distinct from M1's OrderAnalyticsDTO — adds ordersByStatus breakdown.
 */
public class OrderAnalyticsDashboardDTO {

    private long              totalOrders;
    private double            totalRevenue;
    private double            averageOrderValue;
    private double            completionRate;
    private Map<String, Long> ordersByStatus;

    public OrderAnalyticsDashboardDTO() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public long              getTotalOrders()                          { return totalOrders; }
    public void              setTotalOrders(long v)                   { this.totalOrders = v; }

    public double            getTotalRevenue()                         { return totalRevenue; }
    public void              setTotalRevenue(double v)                { this.totalRevenue = v; }

    public double            getAverageOrderValue()                    { return averageOrderValue; }
    public void              setAverageOrderValue(double v)           { this.averageOrderValue = v; }

    public double            getCompletionRate()                       { return completionRate; }
    public void              setCompletionRate(double v)              { this.completionRate = v; }

    public Map<String, Long> getOrdersByStatus()                       { return ordersByStatus; }
    public void              setOrdersByStatus(Map<String, Long> v)   { this.ordersByStatus = v; }

    // ── DP-4: Builder ─────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final OrderAnalyticsDashboardDTO dto = new OrderAnalyticsDashboardDTO();

        public Builder totalOrders(long v)              { dto.totalOrders = v;       return this; }
        public Builder totalRevenue(double v)           { dto.totalRevenue = v;      return this; }
        public Builder averageOrderValue(double v)      { dto.averageOrderValue = v; return this; }
        public Builder completionRate(double v)         { dto.completionRate = v;    return this; }
        public Builder ordersByStatus(Map<String,Long> v){ dto.ordersByStatus = v;  return this; }

        public OrderAnalyticsDashboardDTO build()       { return dto; }
    }
}
