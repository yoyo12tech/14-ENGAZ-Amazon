package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record ProductRatedEvent
        (Long productId, Double newAverage, Integer totalRatings) implements Serializable {}
