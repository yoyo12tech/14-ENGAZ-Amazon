package com.team14.amazon.events;

public record UserRegisteredEvent(Long userId, String email, String role) {}
