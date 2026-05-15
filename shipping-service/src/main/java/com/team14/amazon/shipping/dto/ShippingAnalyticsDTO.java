package com.team14.amazon.shipping.dto;

import com.team14.amazon.shipping.dto.builder.Buildable;

import java.util.Map;


public class ShippingAnalyticsDTO {
    private Long totalShipments;
    private Long deliveredCount;
    private Double averageDeliveryTimeDays;
    private Double onTimeRate;
    private Map<String, Long> shipmentsByStatus;
    private Double averageAttempts;

    private ShippingAnalyticsDTO(Builder builder){
        this.totalShipments = builder.totalShipments;
        this.averageDeliveryTimeDays = builder.averageDeliveryTimeDays;
        this.onTimeRate = builder.onTimeRate;
        this.deliveredCount = builder.deliveredCount;
        this.shipmentsByStatus = builder.shipmentsByStatus;
        this.averageAttempts = builder.averageAttempts;
    }

    public ShippingAnalyticsDTO(){}

    public static Builder builder(){ return new Builder();}

    public Long getTotalShipments() {
        return totalShipments;
    }

    public Long getDeliveredCount() {
        return deliveredCount;
    }

    public Double getAverageDeliveryTimeDays() {
        return averageDeliveryTimeDays;
    }

    public Double getOnTimeRate() {
        return onTimeRate;
    }

    public Map<String, Long> getShipmentsByStatus() {
        return shipmentsByStatus;
    }

    public Double getAverageAttempts() {
        return averageAttempts;
    }

    public static class Builder implements Buildable<ShippingAnalyticsDTO>{
        private Long totalShipments;
        private Long deliveredCount;
        private Double averageDeliveryTimeDays;
        private Double onTimeRate;
        private Map<String, Long> shipmentsByStatus;
        private Double averageAttempts;

        public Builder totalShipments(Long totalShipments){
            this.totalShipments = totalShipments;
            return this;
        }

        public Builder deliveredCount(Long deliveredCount){
            this.deliveredCount = deliveredCount;
            return this;
        }

        public Builder averageDeliveryTimeDays(Double averageDeliveryTimeDays){
            this.averageDeliveryTimeDays = averageDeliveryTimeDays;
            return this;
        }

        public Builder onTimeRate(Double onTimeRate){
            this.onTimeRate = onTimeRate;
            return this;
        }

        public Builder shipmentsByStatus(Map<String, Long> shipmentsByStatus){
            this.shipmentsByStatus = shipmentsByStatus;
            return this;
        }

        public Builder averageAttempts(Double averageAttempts){
            this.averageAttempts = averageAttempts;
            return this;
        }

        @Override
        public ShippingAnalyticsDTO build() { return new ShippingAnalyticsDTO(this);}
    }
}
