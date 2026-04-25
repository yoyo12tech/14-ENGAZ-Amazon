package com.team14.amazon.shipping.dto;

import com.team14.amazon.shipping.dto.builder.Buildable;

import java.time.LocalDateTime;
import java.util.concurrent.Delayed;

public class DelayedShipmentDTO {
    private Long shipmentId;
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private LocalDateTime estimatedDelivery;
    private Long daysOverdue;
    private Integer deliveryAttempts;

//    public DelayedShipmentDTO(Long shipmentId, Long orderId, String carrier, String trackingNumber,
//                              LocalDateTime estimatedDelivery, Long daysOverdue, Integer deliveryAttempts) {
//        this.shipmentId = shipmentId;
//        this.orderId = orderId;
//        this.carrier = carrier;
//        this.trackingNumber = trackingNumber;
//        this.estimatedDelivery = estimatedDelivery;
//        this.daysOverdue = daysOverdue;
//        this.deliveryAttempts = deliveryAttempts;
//    }

    private DelayedShipmentDTO (Builder builder){
        this.shipmentId = builder.shipmentId;
        this.orderId = builder.orderId;
        this.carrier = builder.carrier;
        this.trackingNumber = builder.trackingNumber;
        this.estimatedDelivery = builder.estimatedDelivery;
        this.daysOverdue = builder.daysOverdue;
        this.deliveryAttempts = builder.deliveryAttempts;
    }

    public static Builder builder() {
        return new Builder();
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

    public static class Builder implements Buildable<DelayedShipmentDTO>{
        private Long shipmentId;
        private Long orderId;
        private String carrier;
        private String trackingNumber;
        private LocalDateTime estimatedDelivery;
        private Long daysOverdue;
        private Integer deliveryAttempts;

        public Builder shipmentId(Long shipmentId){
            this.shipmentId = shipmentId;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder carrier(String carrier){
            this.carrier = carrier;
            return this;
        }

        public Builder trackingNumber(String trackingNumber){
            this.trackingNumber = trackingNumber;
            return this;
        }

        public Builder estimatedDelivery(LocalDateTime estimatedDelivery){
            this.estimatedDelivery = estimatedDelivery;
            return this;
        }

        public Builder daysOverdue(Long daysOverdue){
            this.daysOverdue = daysOverdue;
            return this;
        }

        public Builder deliveryAttempts(Integer deliveryAttempts){
            this.deliveryAttempts = deliveryAttempts;
            return this;
        }

        @Override
        public DelayedShipmentDTO build(){
            return new DelayedShipmentDTO(this);
        }
    }
}
