package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record ProductReviewAddedEvent
        (Long productId, Long reviewId, Long userId, Integer rating) implements Serializable {}
