package com.team14.amazon.product.factory;

import com.team14.amazon.product.model.mongodb.EventType;
import com.team14.amazon.product.model.mongodb.MongoEvent;
import com.team14.amazon.product.model.mongodb.ProductEvent;

import java.time.LocalDateTime;
import java.util.Map;

public class EventFactory {

    private EventFactory() {
    }

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type == EventType.PRODUCT) {
            ProductEvent event = new ProductEvent();

            Object pid = params.get("productId");
            if (pid instanceof Number) {
                event.setProductId(((Number) pid).longValue());
            }

            event.setAction((String) params.get("action"));

            Object ts = params.get("timestamp");
            event.setTimestamp(ts instanceof LocalDateTime ? (LocalDateTime) ts : LocalDateTime.now());

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) params.get("details");
            event.setDetails(details);
            return event;
        }

        throw new IllegalArgumentException("Unsupported EventType for product-service: " + type);
    }
}

