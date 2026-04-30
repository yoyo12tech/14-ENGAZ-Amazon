package com.team14.amazon.product.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}

