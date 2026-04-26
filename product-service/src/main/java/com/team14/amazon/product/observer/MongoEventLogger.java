package com.team14.amazon.product.observer;

import com.team14.amazon.product.factory.EventFactory;
import com.team14.amazon.product.model.mongodb.EventType;
import com.team14.amazon.product.model.mongodb.MongoEvent;
import com.team14.amazon.product.model.mongodb.ProductEvent;
import com.team14.amazon.product.repository.mongodb.ProductEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);
    private final ProductEventRepository productEventRepository;
    private final EventType boundEventType = EventType.PRODUCT;

    public MongoEventLogger(ProductEventRepository productEventRepository) {
        this.productEventRepository = productEventRepository;
    }

    @Override
    public void onEvent(String action, Object payload) {
        try {
            if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = new HashMap<>((Map<String, Object>) payload);
                params.put("action", action);

                MongoEvent event = EventFactory.createEvent(boundEventType, params);
                if (event instanceof ProductEvent) {
                    productEventRepository.save((ProductEvent) event);
                }
            }
        } catch (Exception e) {
            // Soft dependency: event logging must not break write operations.
            log.warn("Failed to log product event to MongoDB: {}", e.getMessage());
        }
    }
}

