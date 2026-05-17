package com.team14.amazon.shipping.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public class ShipmentTrackingDTO {
    private LocalDateTime timestamp;
    private String status;
    private String carrier;
    private String trackingNumber;
    private Double latitude;
    private Double longitude;
    private String notes;

    public ShipmentTrackingDTO() {}

    public ShipmentTrackingDTO(LocalDateTime timestamp, String status, String carrier, String trackingNumber, Double latitude, Double longitude, String notes) {
        this.timestamp = timestamp;
        this.status = status;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public static ShipmentTrackingDTOBuilder builder() {
        return new ShipmentTrackingDTOBuilder();
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public static class ShipmentTrackingDTOBuilder {
        private LocalDateTime timestamp;
        private String status;
        private String carrier;
        private String trackingNumber;
        private Double latitude;
        private Double longitude;
        private String notes;

        ShipmentTrackingDTOBuilder() {}

        public ShipmentTrackingDTOBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ShipmentTrackingDTOBuilder status(String status) { this.status = status; return this; }
        public ShipmentTrackingDTOBuilder carrier(String carrier) { this.carrier = carrier; return this; }
        public ShipmentTrackingDTOBuilder trackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; return this; }
        public ShipmentTrackingDTOBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public ShipmentTrackingDTOBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public ShipmentTrackingDTOBuilder notes(String notes) { this.notes = notes; return this; }

        public ShipmentTrackingDTO build() {
            return new ShipmentTrackingDTO(timestamp, status, carrier, trackingNumber, latitude, longitude, notes);
        }
    }
}