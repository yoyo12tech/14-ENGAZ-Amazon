package com.team14.amazon.user.dto;

public class UserOrderSummaryDTO {
    private Long userId;
    private String name;
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Double totalSpent;
    private Double averageOrderValue;


    public UserOrderSummaryDTO(Long userId, String name, Long totalOrders,Long completedOrders, Long cancelledOrders,Double totalSpent, Double averageOrderValue) {
        this.userId = userId;
        this.name = name;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        this.totalSpent = totalSpent;
        this.averageOrderValue = averageOrderValue;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Long completedOrders) { this.completedOrders = completedOrders; }

    public Long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(Long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public Double getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(Double averageOrderValue) { this.averageOrderValue = averageOrderValue; }
}