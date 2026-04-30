package com.team14.amazon.user.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ActivityFeedDTO {
    private List<ActivityEventDTO> content;
    private int page;
    private int size;
    private long totalElements;

    public ActivityFeedDTO() {}

    public ActivityFeedDTO(List<ActivityEventDTO> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
    }

    public List<ActivityEventDTO> getContent() { return content; }
    public void setContent(List<ActivityEventDTO> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    // --- Inner DTO for each event entry ---
    public static class ActivityEventDTO {
        private String action;
        private LocalDateTime timestamp;
        private Map<String, Object> details;

        public ActivityEventDTO() {}

        public ActivityEventDTO(String action, LocalDateTime timestamp, Map<String, Object> details) {
            this.action = action;
            this.timestamp = timestamp;
            this.details = details;
        }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
