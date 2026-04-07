package com.team14.amazon.shipping.dto;

public class CarrierSummaryDTO {
    private String carrier;
    private Long totalShipments;
    private Long deliveredCount;
    private Double averageDeliveryDays;
    private Double onTimeRate;

    // Standard Constructor
    public CarrierSummaryDTO(String carrier, Long totalShipments, Long deliveredCount, Double averageDeliveryDays, Double onTimeRate) {
        this.carrier = carrier;
        this.totalShipments = totalShipments;
        this.deliveredCount = deliveredCount;
        this.averageDeliveryDays = averageDeliveryDays;
        this.onTimeRate = onTimeRate;
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
}
