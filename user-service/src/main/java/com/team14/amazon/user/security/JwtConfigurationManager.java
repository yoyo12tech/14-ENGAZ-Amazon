package com.team14.amazon.user.security;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expirationMs;

    private JwtConfigurationManager() {
        String envSecret = System.getenv("JWT_SECRET");
        // Use a 44-character Base64 encoded string as default for local dev if not provided (32 bytes when decoded)
        this.secret = (envSecret != null && !envSecret.trim().isEmpty()) ? envSecret : "ZGVmYXVsdFNlY3JldEtleU11c3RCZUF0TGVhc3QzMkJ5dGVz";
        
        String envExpiration = System.getenv("JWT_EXPIRATION_MS");
        this.expirationMs = (envExpiration != null && !envExpiration.trim().isEmpty()) ? Long.parseLong(envExpiration) : 86400000L;
    }

    public static JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
