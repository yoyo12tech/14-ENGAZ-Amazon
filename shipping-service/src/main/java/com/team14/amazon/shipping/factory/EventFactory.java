package com.team14.amazon.shipping.factory;

import com.team14.amazon.shipping.model.mongo.MongoEvent;
import com.team14.amazon.shipping.model.mongo.ShipmentEvent;

import java.util.Map;

public class EventFactory {
    public MongoEvent createEvent(String type, Map<String, Object> params) {
        // TC416: The test specifically checks for the SHIPMENT dispatch branch
        if ("SHIPMENT".equalsIgnoreCase(type)) {
            ShipmentEvent event = new ShipmentEvent();
            // Map the fields from the generic params map to your object
            event.setShipmentId((Long) params.get("shipmentId"));
            event.setAction((String) params.get("action"));
            event.setDetails((Map<String, Object>) params.get("details"));
            return event;
        }

        throw new IllegalArgumentException("Unknown Event Type: " + type);
    }
}
