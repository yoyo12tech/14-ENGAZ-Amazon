package com.team14.amazon.order.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MongoDB document for order lifecycle events.
 * Collection: order_events
 *
 * DP-6 Factory Pattern : created only via EventFactory.createEvent(ORDER, params)
 * DP-2 Observer Pattern: written only by MongoEventLogger.onEvent()
 *
 * Action values (non-exhaustive):
 *   ANALYTICS_VIEWED, INTERACTION_RECORDED,
 *   ORDER_CONFIRMED, ORDER_DELIVERED, ORDER_CANCELLED,
 *   ITEMS_ADDED, ORDER_CREATED, ORDER_DELETED,
 *   ORDER_ITEM_CREATED
 */
@Document(collection = "order_events")
public class OrderEvent implements MongoEvent {

    @Id
    private String id;

    private Long orderId;

    private String action;

    private LocalDateTime timestamp;

    private Map<String, Object> details = new HashMap<>();

    public OrderEvent() {}

    public OrderEvent(Long orderId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        this.orderId    = orderId;
        this.action     = action;
        this.timestamp  = timestamp;
        this.details    = details != null ? details : new HashMap<>();
    }

    @Override public String getId()                    { return id; }
    public void setId(String id)                       { this.id = id; }

    public Long getOrderId()                           { return orderId; }
    public void setOrderId(Long orderId)               { this.orderId = orderId; }

    @Override public String getAction()                { return action; }
    public void setAction(String action)               { this.action = action; }

    @Override public LocalDateTime getTimestamp()      { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp)  { this.timestamp = timestamp; }

    @Override public Map<String, Object> getDetails()  { return details; }
    public void setDetails(Map<String, Object> details){ this.details = details; }
}
