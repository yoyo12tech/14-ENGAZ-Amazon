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
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder{
        private LocalDateTime timestamp;
        private Source source;
        private String action;
        private Map<String, Object> details;


        public Builder timeStamp(LocalDateTime timestamp){
            this.timestamp = timestamp;
            return this;
        }

        public Builder source(Source source){
            this.source = source;
            return this;
        }

        public Builder action(String action){
            this.action = action;
            return this;
        }

        public Builder details(Map<String, Object> details){
            this.details = details;
            return this;
        }

        public LifecycleEventDTO build(){
            return new LifecycleEventDTO(timestamp, source, action, details);
        }
    }
}

