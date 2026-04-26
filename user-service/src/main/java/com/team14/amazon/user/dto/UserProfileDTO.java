package com.team14.amazon.user.dto;

import com.team14.amazon.user.model.ShippingAddress;
import java.util.List;
import java.util.Map;

public class UserProfileDTO {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Map<String, Object> preferences;
    private List<ShippingAddress> shippingAddresses;
    private int totalAddresses;

    // Default Constructor
    public UserProfileDTO() {}

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }

    public List<ShippingAddress> getShippingAddresses() { return shippingAddresses; }
    public void setShippingAddresses(List<ShippingAddress> shippingAddresses) { this.shippingAddresses = shippingAddresses; }

    public int getTotalAddresses() { return totalAddresses; }
    public void setTotalAddresses(int totalAddresses) { this.totalAddresses = totalAddresses; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private String email;
        private String phone;
        private Map<String, Object> preferences;
        private List<com.team14.amazon.user.model.ShippingAddress> shippingAddresses;
        private int totalAddresses;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public Builder shippingAddresses(List<com.team14.amazon.user.model.ShippingAddress> shippingAddresses) {
            this.shippingAddresses = shippingAddresses;
            return this;
        }

        public Builder totalAddresses(int totalAddresses) {
            this.totalAddresses = totalAddresses;
            return this;
        }

        public UserProfileDTO build() {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setUserId(userId);
            dto.setName(name);
            dto.setEmail(email);
            dto.setPhone(phone);
            dto.setPreferences(preferences);
            dto.setShippingAddresses(shippingAddresses);
            dto.setTotalAddresses(totalAddresses);
            return dto;
        }
    }
    }