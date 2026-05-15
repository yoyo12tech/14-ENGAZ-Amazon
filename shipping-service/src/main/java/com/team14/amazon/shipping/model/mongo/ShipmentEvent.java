package com.team14.amazon.shipping.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "shipment_events")
public class ShipmentEvent implements MongoEvent {

    @Id
    private String id;
    private Long shipmentId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public ShipmentEvent(){

    }

    public ShipmentEvent(Long shipmentId, String action, Map<String, Object> details) {
        this.shipmentId = shipmentId;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public Long getShipmentId() { return shipmentId; }
    public String getAction() { return action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getDetails() { return details; }


    public void setId(String id) {
        this.id = id;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}