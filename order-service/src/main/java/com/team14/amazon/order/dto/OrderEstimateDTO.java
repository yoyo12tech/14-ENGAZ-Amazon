package com.team14.amazon.order.dto;

public class OrderEstimateDTO {
    private int itemCount;
    private double subtotal;
    private double shippingCost;
    private double estimatedTotal;
    private double discountApplied;

    public OrderEstimateDTO(int itemCount, double subtotal, double shippingCost, double estimatedTotal, double discountApplied) {
        this.itemCount = itemCount;
        this.subtotal = subtotal;
        this.shippingCost = shippingCost;
        this.estimatedTotal = estimatedTotal;
        this.discountApplied = discountApplied;
    }

    // Getters & setters
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getShippingCost() { return shippingCost; }
    public void setShippingCost(double shippingCost) { this.shippingCost = shippingCost; }

    public double getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(double estimatedTotal) { this.estimatedTotal = estimatedTotal; }

    public double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(double discountApplied) { this.discountApplied = discountApplied; }
}