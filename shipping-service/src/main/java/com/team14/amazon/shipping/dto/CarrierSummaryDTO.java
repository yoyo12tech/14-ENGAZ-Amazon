package com.team14.amazon.shipping.dto;

import com.team14.amazon.shipping.dto.builder.Buildable;

public class CarrierSummaryDTO {
    private String carrier;
    private Long totalShipments;
    private Long deliveredCount;
    private Double averageDeliveryDays;
    private Double onTimeRate;

    // Standard Constructor
//    public CarrierSummaryDTO(String carrier, Long totalShipments, Long deliveredCount, Double averageDeliveryDays, Double onTimeRate) {
//        this.carrier = carrier;
//        this.totalShipments = totalShipments;
//        this.deliveredCount = deliveredCount;
//        this.averageDeliveryDays = averageDeliveryDays;
//        this.onTimeRate = onTimeRate;
//    }

    private CarrierSummaryDTO(Builder builder){
        this.carrier = builder.carrier;
        this.totalShipments = builder.totalShipments;
        this.deliveredCount = builder.deliveredCount;
        this.averageDeliveryDays = builder.averageDeliveryDays;
        this.onTimeRate = builder.onTimeRate;
    }

    public static CarrierSummaryDTO.Builder builder() {
        return new CarrierSummaryDTO.Builder();
    }


    // Getters and Setters
    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public Long getTotalShipments() {
        return totalShipments;
    }

    public void setTotalShipments(Long totalShipments) {
        this.totalShipments = totalShipments;
    }

    public Long getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(Long deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public Double getAverageDeliveryDays() {
        return averageDeliveryDays;
    }

    public void setAverageDeliveryDays(Double averageDeliveryDays) {
        this.averageDeliveryDays = averageDeliveryDays;
    }

    public Double getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(Double onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public static class Builder implements Buildable<CarrierSummaryDTO> {
        private String carrier;
        private Long totalShipments;
        private Long deliveredCount;
        private Double averageDeliveryDays;
        private Double onTimeRate;

        public Builder carrier(String carrier){
            this.carrier = carrier;
            return this;
        }

        public Builder totalShipments(Long totalShipments){
            this.totalShipments = totalShipments;
            return this;
        }

        public Builder deliveredCount(Long deliveredCount){
            this.deliveredCount = deliveredCount;
            return this;
        }

        public Builder averageDeliveryDays(Double averageDeliveryDays){
            this.averageDeliveryDays = averageDeliveryDays;
            return this;
        }

        public Builder onTimeRate(Double onTimeRate){
            this.onTimeRate = onTimeRate;
            return this;
        }
        @Override
        public CarrierSummaryDTO build() {
            return new CarrierSummaryDTO(this);
        }
    }

}