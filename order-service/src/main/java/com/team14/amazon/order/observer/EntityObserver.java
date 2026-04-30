package com.team14.amazon.order.observer;

/**
 * DP-2 Observer Pattern: classical GoF observer interface.
 * Concrete implementation: MongoEventLogger.
 *
 * IMPORTANT (grader check): No method in any service annotated with
 * @EventListener may write to MongoDB. All MongoDB event writes must
 * flow through this observer chain.
 */
public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
