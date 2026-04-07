package com.team14.amazon.shipping.dto;

import java.time.LocalDateTime;

public class DelayedShipmentDTO {
    private Long shipmentId;
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private LocalDateTime estimatedDelivery;
    private Long daysOverdue;
    private Integer deliveryAttempts;

    public DelayedShipmentDTO(Long shipmentId, Long orderId, String carrier, String trackingNumber,
                              LocalDateTime estimatedDelivery, Long daysOverdue, Integer deliveryAttempts) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.estimatedDelivery = estimatedDelivery;
        this.daysOverdue = daysOverdue;
        this.deliveryAttempts = deliveryAttempts;
    }

    public Long getShipmentId() { return shipmentId; }
    public Long getOrderId() { return orderId; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public Long getDaysOverdue() { return daysOverdue; }
    public Integer getDeliveryAttempts() { return deliveryAttempts; }

    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    public void setDaysOverdue(Long daysOverdue) { this.daysOverdue = daysOverdue; }
    public void setDeliveryAttempts(Integer deliveryAttempts) { this.deliveryAttempts = deliveryAttempts; }
}