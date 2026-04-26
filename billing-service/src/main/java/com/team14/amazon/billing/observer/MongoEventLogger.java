package com.team14.amazon.billing.observer;

import com.team14.amazon.billing.factory.EventFactory;
import com.team14.amazon.billing.model.mongodb.EventType;
import com.team14.amazon.billing.model.mongodb.MongoEvent;
import com.team14.amazon.billing.model.mongodb.TransactionAuditEvent;
import com.team14.amazon.billing.repository.mongodb.TransactionAuditEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);
    private final TransactionAuditEventRepository repository;
    private final EventType boundEventType = EventType.TRANSACTION_AUDIT;

    public MongoEventLogger(TransactionAuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onEvent(String action, Object payload) {
        try {
            if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = new HashMap<>((Map<String, Object>) payload);

                params.put("action", action);

                MongoEvent event = EventFactory.createEvent(boundEventType, params);

                if (event instanceof TransactionAuditEvent) {
                    repository.save((TransactionAuditEvent) event);
                }
            }
        } catch (Exception e) {
            log.warn("MongoDB write failed for action: {}", action, e);
        }
    }
}