package com.team14.amazon.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Spring @Service for JWT token validation.
 * Order-service only validates tokens issued by user-service — it never generates them.
 *
 * Obtains config via JwtConfigurationManager.getInstance() (NOT @Autowired),
 * so the GoF Singleton is properly exercised.
 *
 * Uses the SAME secret as user-service (shared via env var / application.yml).
 */
@Service
public class JwtService {

    private final JwtConfigurationManager configManager = JwtConfigurationManager.getInstance();

    private SecretKey getSigningKey() {
        byte[] keyBytes = configManager.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("uid");
        return uid != null ? ((Number) uid).longValue() : null;
    }

    public String extractRole(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    public long getExpirationMs() {
        return configManager.getExpirationMs();
    }
}
