package com.team14.amazon.shipping.dto;

public class NearbyShipmentDTO {
    private Long shipmentId;
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;

    public NearbyShipmentDTO(Long shipmentId, Long orderId, String carrier,
                             String trackingNumber, Double latitude,
                             Double longitude, Double distanceKm) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
    }

    public Long getShipmentId() { return shipmentId; }
    public Long getOrderId() { return orderId; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getDistanceKm() { return distanceKm; }
}