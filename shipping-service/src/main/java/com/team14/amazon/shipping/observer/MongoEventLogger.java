package com.team14.amazon.shipping.observer;

import com.team14.amazon.shipping.model.mongo.ShipmentEvent;
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
    public void onEvent(String action, Long shipmentId, Map<String, Object> details) {
        try {
            ShipmentEvent event = new ShipmentEvent(shipmentId, action, details);
            mongoTemplate.save(event);
        } catch (Exception e) {
            log.warn("Failed to log shipment event to MongoDB: {}", e.getMessage());
        }
    }
}