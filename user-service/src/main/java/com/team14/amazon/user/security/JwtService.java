package com.team14.amazon.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {


    public String generateToken(String email, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role);

        return createToken(claims, email);
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

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        long expirationMs = JwtConfigurationManager.getInstance().getExpirationMs();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.valueOf(JwtConfigurationManager.getInstance().getAlgorithm()))
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = JwtConfigurationManager.getInstance().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Long extractUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().get("uid", Long.class);
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().get("role", String.class);
    }

    public long getExpirationMs() {
        return JwtConfigurationManager.getInstance().getExpirationMs();
    }
}
