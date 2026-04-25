package com.team14.amazon.shipping.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
