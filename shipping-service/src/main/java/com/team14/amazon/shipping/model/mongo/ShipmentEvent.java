package com.team14.amazon.shipping.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "shipment_events")
public class ShipmentEvent {

    @Id
    private String id;
    private String eventType;
    private Object payload;
    private LocalDateTime timestamp;

    public ShipmentEvent(String eventType, Object payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public Object getPayload() { return payload; }
    public LocalDateTime getTimestamp() { return timestamp; }
}