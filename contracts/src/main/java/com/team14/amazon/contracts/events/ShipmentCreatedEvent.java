package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record ShipmentCreatedEvent
        (Long shipmentId, Long orderId, String carrier, String trackingNumber) implements Serializable {}
