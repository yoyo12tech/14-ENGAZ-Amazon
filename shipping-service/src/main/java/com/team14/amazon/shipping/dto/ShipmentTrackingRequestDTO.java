package com.team14.amazon.shipping.dto;
import com.team14.amazon.shipping.dto.builder.Buildable;

public class ShipmentTrackingRequestDTO {
    
    private String status;
    private Double latitude;
    private Double longitude;
    private String notes;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
