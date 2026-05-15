package com.team14.amazon.shipping.security;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public final class JwtConfigurationManager {

    private static final JwtConfigurationManager INSTANCE = new JwtConfigurationManager();

    private final String secret;

    private JwtConfigurationManager() {
        this.secret = resolveSecret();
    }

    private static String resolveSecret() {
        String fromEnv = System.getenv("JWT_SECRET");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return readSecretFromApplicationYml();
    }

    public static JwtConfigurationManager getInstance() {
        return INSTANCE;
    }

    public String getSecret() {
        return secret;
    }

    @SuppressWarnings("unchecked")
    private static String readSecretFromApplicationYml() {
        try (InputStream in = JwtConfigurationManager.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (in == null) {
                throw new IllegalStateException("application.yml not found on classpath");
            }
            Map<String, Object> root = new Yaml().load(in);
            if (root == null) {
                throw new IllegalStateException("application.yml is empty");
            }

            // Primary location: jwt.secret (same as user-service/product-service).
            Object jwtObj = root.get("jwt");
            if (jwtObj instanceof Map) {
                Map<String, Object> jwt = (Map<String, Object>) jwtObj;
                Object secretObj = jwt.get("secret");
                if (secretObj != null && !String.valueOf(secretObj).isBlank()) {
                    return String.valueOf(secretObj);
                }
            }

            // Backward compatibility with older billing config: data.jwt.secret.
            Object dataObj = root.get("data");
            if (!(dataObj instanceof Map)) {
                throw new IllegalStateException("jwt.secret missing in application.yml");
            }
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object nestedJwtObj = data.get("jwt");
            if (!(nestedJwtObj instanceof Map)) {
                throw new IllegalStateException("jwt.secret missing in application.yml");
            }
            Map<String, Object> nestedJwt = (Map<String, Object>) nestedJwtObj;
            Object secretObj = nestedJwt.get("secret");
            if (secretObj == null || String.valueOf(secretObj).isBlank()) {
                throw new IllegalStateException("jwt.secret must not be blank");
            }

            return String.valueOf(secretObj);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load jwt.secret from application.yml", e);
        }
    }
}
