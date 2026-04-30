package com.team14.amazon.billing.dto;


import com.team14.amazon.billing.model.Source;

import java.time.LocalDateTime;
import java.util.Map;

public record LifecycleEventDTO(
        LocalDateTime timestamp,
        Source source,
        String action,
        Map<String, Object> details
) {
    //----IMPORTANT: Check if this is Correct or not with the Scrum----
    public static TransactionDetailsDTO.Builder builder() {
        return new TransactionDetailsDTO.Builder();
    }
}
