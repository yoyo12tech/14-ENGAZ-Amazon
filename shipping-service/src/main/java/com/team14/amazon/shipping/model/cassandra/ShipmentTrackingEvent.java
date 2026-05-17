package com.team14.amazon.shipping.model.cassandra;
import com.team14.amazon.shipping.model.Shipment;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("shipment_tracking_events")
public class ShipmentTrackingEvent {

    @PrimaryKeyColumn(name = "shipment_id", type = PrimaryKeyType.PARTITIONED)
    private Long shipmentId;

    // CLUSTERING KEY: Sorts events newest-first within the partition
    @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant timestamp;

    @Column("status")
    private Shipment.Status status;

    @Column("carrier")
    private String carrier;

    @Column("tracking_number")
    private String trackingNumber;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    @Column("notes")
    private String notes;

    public ShipmentTrackingEvent() {}

    public ShipmentTrackingEvent(Long shipmentId, Instant timestamp, Shipment.Status status, String carrier, String trackingNumber, Double latitude, Double longitude, String notes) {
        this.shipmentId = shipmentId;
        this.timestamp = timestamp;
        this.status = status;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public static ShipmentTrackingEventBuilder builder() {
        return new ShipmentTrackingEventBuilder();
    }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Shipment.Status getStatus() { return status; }
    public void setStatus(Shipment.Status status) { this.status = status; }

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

    public static class ShipmentTrackingEventBuilder {
        private Long shipmentId;
        private Instant timestamp;
        private Shipment.Status status;
        private String carrier;
        private String trackingNumber;
        private Double latitude;
        private Double longitude;
        private String notes;

        ShipmentTrackingEventBuilder() {}

        public ShipmentTrackingEventBuilder shipmentId(Long shipmentId) { this.shipmentId = shipmentId; return this; }
        public ShipmentTrackingEventBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public ShipmentTrackingEventBuilder status(Shipment.Status status) { this.status = status; return this; }
        public ShipmentTrackingEventBuilder carrier(String carrier) { this.carrier = carrier; return this; }
        public ShipmentTrackingEventBuilder trackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; return this; }
        public ShipmentTrackingEventBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public ShipmentTrackingEventBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public ShipmentTrackingEventBuilder notes(String notes) { this.notes = notes; return this; }

        public ShipmentTrackingEvent build() {
            return new ShipmentTrackingEvent(shipmentId, timestamp, status, carrier, trackingNumber, latitude, longitude, notes);
        }
    }
}

