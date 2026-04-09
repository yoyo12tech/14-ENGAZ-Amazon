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
}