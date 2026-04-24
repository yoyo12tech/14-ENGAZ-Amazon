package com.team14.amazon.user.observer;

import com.team14.amazon.user.factory.EventFactory;
import com.team14.amazon.user.model.mongodb.AuthEvent;
import com.team14.amazon.user.model.mongodb.EventType;
import com.team14.amazon.user.model.mongodb.MongoEvent;
import com.team14.amazon.user.repository.mongodb.AuthEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);
    private final AuthEventRepository authEventRepository;
    private final EventType boundEventType = EventType.AUTH;

    public MongoEventLogger(AuthEventRepository authEventRepository) {
        this.authEventRepository = authEventRepository;
    }

    @Override
    public void onEvent(String action, Object payload) {
        try {
            if (payload instanceof Map) {
                Map<String, Object> params = new HashMap<>((Map<String, Object>) payload);
                params.put("action", action);
                
                MongoEvent event = EventFactory.createEvent(boundEventType, params);
                
                if (event instanceof AuthEvent) {
                    authEventRepository.save((AuthEvent) event);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to log event to MongoDB: {}", e.getMessage());
        }
    }
}
