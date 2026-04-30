package com.team14.amazon.order.observer;

import com.team14.amazon.order.factory.EventFactory;
import com.team14.amazon.order.model.mongo.EventType;
import com.team14.amazon.order.model.mongo.MongoEvent;
import com.team14.amazon.order.model.mongo.OrderEvent;
import com.team14.amazon.order.repository.OrderEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * DP-2: Observer Pattern — concrete observer.
 * DP-6: Factory Pattern — uses EventFactory to create the correct MongoEvent subtype.
 *
 * Bound to EventType.ORDER for the order-service.
 * Failure policy: catches all Mongo exceptions and logs at WARN — never rethrows.
 * The upstream PostgreSQL transaction must not roll back on a MongoDB write failure.
 */
@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    /** Fixed event type for this service's observer instance. */
    private final EventType boundEventType = EventType.ORDER;

    private final OrderEventRepository orderEventRepository;

    public MongoEventLogger(OrderEventRepository orderEventRepository) {
        this.orderEventRepository = orderEventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", eventType);
            params.put("timestamp", LocalDateTime.now());

            if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = (Map<String, Object>) payload;
                params.putAll(payloadMap);
                // details = the full payload map minus reserved keys
                Map<String, Object> details = new HashMap<>(payloadMap);
                details.remove("orderId");
                details.remove("action");
                details.remove("timestamp");
                params.put("details", details);
            }

            MongoEvent event = EventFactory.createEvent(boundEventType, params);
            orderEventRepository.save((OrderEvent) event);

        } catch (Exception ex) {
            log.warn("[MongoEventLogger] Failed to persist order event '{}': {}", eventType, ex.getMessage());
            // Must NOT rethrow — soft dependency
        }
    }
}
