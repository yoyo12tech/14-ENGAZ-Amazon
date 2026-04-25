package com.team14.amazon.shipping.observer;

import java.util.Map;

public interface EntityObserver {
    void onEvent(String action, Long shipmentId, Map<String, Object> details);
}
