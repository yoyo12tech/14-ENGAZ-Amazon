package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record UserRegisteredEvent(Long userId, String email, String role) implements Serializable {}