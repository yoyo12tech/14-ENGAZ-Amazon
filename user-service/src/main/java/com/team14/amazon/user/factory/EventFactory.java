package com.team14.amazon.user.factory;

import com.team14.amazon.user.model.mongodb.*;
import java.time.LocalDateTime;
import java.util.Map;

public class EventFactory {

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {
        if (type == EventType.AUTH) {
            AuthEvent event = new AuthEvent();
            event.setUserId((Long) params.get("userId"));
            event.setAction((String) params.get("action"));
            event.setTimestamp((LocalDateTime) params.get("timestamp"));
            event.setDetails((Map<String, Object>) params.get("details"));
            return event;
        }
        // Other types will be handled by their respective services
        throw new IllegalArgumentException("Unsupported event type: " + type);
    }
}
