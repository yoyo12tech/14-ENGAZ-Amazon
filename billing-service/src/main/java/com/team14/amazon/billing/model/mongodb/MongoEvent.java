package com.team14.amazon.billing.model.mongodb;

import java.time.LocalDateTime;
import java.util.Map;

public interface MongoEvent {
    String getId();

    LocalDateTime getTimestamp();

    String getAction();

    Map<String, Object> getDetails();
}