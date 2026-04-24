package com.team14.amazon.user.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}
