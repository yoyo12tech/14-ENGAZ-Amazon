package com.team14.amazon.gateway.security;

public final class JwtConfigurationManager {

    private static final JwtConfigurationManager INSTANCE = new JwtConfigurationManager();

    private final String secret;

    private JwtConfigurationManager() {
        this.secret = getEnvOrDefault("JWT_SECRET", "bXlTdXBlclNlY3JldEtleUZvckpXVEF1dGhlbnRpY2F0aW9u");
    }

    public static JwtConfigurationManager getInstance() {
        return INSTANCE;
    }

    public String getSecret() {
        return secret;
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
