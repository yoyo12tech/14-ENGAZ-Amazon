package com.team14.amazon.shipping.factory;

import com.team14.amazon.shipping.model.mongo.MongoEvent;
import com.team14.amazon.shipping.model.mongo.ShipmentEvent;

import java.time.LocalDateTime;
import java.util.Map;

public class EventFactory {
    public MongoEvent createEvent(String type, Map<String, Object> params) {
        return switch (type.toUpperCase()) {
            case "SHIPMENT", "ANALYTICS_VIEWED" -> {
                ShipmentEvent event = new ShipmentEvent();
                event.setAction(type);

                if (params.get("shipmentId") != null) {
                    event.setShipmentId((Long) params.get("shipmentId"));
                }

                event.setTimestamp(params.containsKey("timestamp")
                        ? (LocalDateTime) params.get("timestamp")
                        : LocalDateTime.now());

                // Guard against null details
                Object details = params.get("details");
                event.setDetails(details != null ? (Map<String, Object>) details : Map.of());
                yield event;
            }
            default -> throw new IllegalArgumentException("Unknown Event Type: " + type);
        };
    }
}
