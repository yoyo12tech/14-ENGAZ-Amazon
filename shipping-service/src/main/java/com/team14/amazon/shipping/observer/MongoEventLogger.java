package com.team14.amazon.shipping.observer;

import com.team14.amazon.shipping.factory.EventFactory;
import com.team14.amazon.shipping.model.mongo.MongoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {
    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);
    private final MongoTemplate mongoTemplate;

    public MongoEventLogger(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void onEvent(String action, Object payload) {
        try {
            // Soft Dependency: MongoDB failures must not crash the service
            if (!(payload instanceof Map)) {
                log.warn("Payload is not a Map, cannot log event");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) payload;
            params.put("action", action);

            // DP-6 Factory Pattern: Event creation must go through the factory (static call)
            MongoEvent event = EventFactory.createEvent("SHIPMENT", params);

            // DP-2 Observer Pattern: The logger persists the event to MongoDB
            mongoTemplate.save(event);

        } catch (Exception e) {
            // Soft Dependency Requirement: log at WARN and do not rethrow
            log.warn("Failed to log shipment event to MongoDB (Soft Dependency): {}", e.getMessage());
        }
    }
}