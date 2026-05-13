package com.team14.amazon.contracts.events;

public record UserRegisteredEvent(Long userId, String email, String role) {}
