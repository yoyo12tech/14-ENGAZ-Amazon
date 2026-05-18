package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record ShipmentStatusChangedEvent
        (Long shipmentId, Long orderId, String newStatus) implements Serializable {}
