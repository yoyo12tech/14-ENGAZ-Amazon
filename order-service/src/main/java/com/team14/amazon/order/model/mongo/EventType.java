package com.team14.amazon.order.model.mongo;

/**
 * DP-6 Factory Pattern: event type discriminator used by EventFactory.
 * Order-service is bound to ORDER type.
 */
public enum EventType {
    AUTH, PRODUCT, ORDER, SHIPMENT, TRANSACTION_AUDIT
}
