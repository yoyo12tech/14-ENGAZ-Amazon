package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record ShipmentCancelledEvent
        (Long shipmentId, Long orderId) implements Serializable {}
