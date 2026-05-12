package com.team14.amazon.events;

public record ProductReviewAddedEvent(Long productId, Long reviewId, Long userId, Integer rating) {}
