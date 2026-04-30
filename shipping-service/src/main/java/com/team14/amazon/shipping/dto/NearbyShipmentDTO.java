package com.team14.amazon.shipping.dto;
import com.team14.amazon.shipping.dto.builder.Buildable;

public class NearbyShipmentDTO {
    private Long shipmentId;
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;

    private NearbyShipmentDTO(Builder builder) {
        this.shipmentId = builder.shipmentId;
        this.orderId = builder.orderId;
        this.carrier = builder.carrier;
        this.trackingNumber = builder.trackingNumber;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.distanceKm = builder.distanceKm;
    }

    public static Builder builder() {
        return new Builder();
    }

//    public NearbyShipmentDTO(Long shipmentId, Long orderId, String carrier,
//                             String trackingNumber, Double latitude,
//                             Double longitude, Double distanceKm) {
//        this.shipmentId = shipmentId;
//        this.orderId = orderId;
//        this.carrier = carrier;
//        this.trackingNumber = trackingNumber;
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.distanceKm = distanceKm;
//    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public static class Builder implements Buildable<NearbyShipmentDTO> {
        private Long shipmentId;
        private Long orderId;
        private String carrier;
        private String trackingNumber;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;

        public Builder shipmentId(Long shipmentId) {
            this.shipmentId = shipmentId;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder carrier(String carrier) {
            this.carrier = carrier;
            return this;
        }

        public Builder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        @Override
        public NearbyShipmentDTO build() {
            return new NearbyShipmentDTO(this);
        }
    }
}