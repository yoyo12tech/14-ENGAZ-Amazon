package com.team14.amazon.shipping.factory;

import com.team14.amazon.shipping.model.mongo.MongoEvent;
import com.team14.amazon.shipping.model.mongo.ShipmentEvent;

import java.time.LocalDateTime;
import java.util.Map;

public class EventFactory {
    public MongoEvent createEvent(String type, Map<String, Object> params) {
        return switch (type.toUpperCase()) {
            case "SHIPMENT", "ANALYTICS_VIEWED" -> {
                ShipmentEvent e = new ShipmentEvent();
                e.setShipmentId((Long) params.get("shipmentId"));
                e.setAction((String) params.get("action"));

                if (params.containsKey("timestamp")) {
                    e.setTimestamp((LocalDateTime) params.get("timestamp"));
                } else {
                    e.setTimestamp(LocalDateTime.now());
                }

                e.setDetails((Map<String, Object>) params.get("details"));
                yield e;
            }
            default -> throw new IllegalArgumentException("Unknown Event Type: " + type);
        };
    }
}
