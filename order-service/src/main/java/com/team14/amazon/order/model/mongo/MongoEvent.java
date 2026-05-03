package com.team14.amazon.order.model.mongo;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DP-6 Factory Pattern: common interface for all MongoDB event documents.
 * Matches user-service MongoEvent interface exactly.
 */
public interface MongoEvent {
    String getId();
    LocalDateTime getTimestamp();
    String getAction();
    Map<String, Object> getDetails();
}
