package com.team14.amazon.user.security;

public class JwtConfigurationManager {

    private static final JwtConfigurationManager INSTANCE = new JwtConfigurationManager();

    private final String secret;
    private final long expirationMs;
    private final String algorithm;

    private JwtConfigurationManager() {
        // Read environment variables with fallback defaults
        this.secret = getEnvOrDefault("JWT_SECRET", "v9y$B&E)H@MbQeThWmZq4t7w!z%C*F-JaNdRfUjXn2r5");
        this.expirationMs = Long.parseLong(getEnvOrDefault("JWT_EXPIRATION_MS", "86400000")); // 24 hours
        this.algorithm = getEnvOrDefault("JWT_ALGORITHM", "HS256");
    }

    public static JwtConfigurationManager getInstance() {
        return INSTANCE;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
