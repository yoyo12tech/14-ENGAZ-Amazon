package com.team14.amazon.billing.factory;

import org.springframework.stereotype.Component;

import com.team14.amazon.billing.model.mongodb.EventType;
import com.team14.amazon.billing.model.mongodb.MongoEvent;
import com.team14.amazon.billing.model.mongodb.TransactionAuditEvent;

import java.time.LocalDateTime;
import java.util.Map;

public final class EventFactory {

    private EventFactory() {}

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type == EventType.TRANSACTION_AUDIT) {

            Long transactionId = (Long) params.get("transactionId");
            String action = (String) params.get("action");
            String method = (String) params.get("method");
            Double amount = (Double) params.get("amount");

            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) params.get("details");

            LocalDateTime timestamp = params.containsKey("timestamp")
                    ? (LocalDateTime) params.get("timestamp")
                    : LocalDateTime.now();

            return new TransactionAuditEvent(null, transactionId, action, timestamp, method, amount, details);
        }

        throw new IllegalArgumentException("Unsupported event type: " + type);
    }
}