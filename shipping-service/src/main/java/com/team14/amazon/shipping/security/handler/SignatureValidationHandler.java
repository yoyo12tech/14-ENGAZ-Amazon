package com.team14.amazon.shipping.security.handler;

import com.team14.amazon.shipping.security.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class SignatureValidationHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) {
        String secret = JwtConfigurationManager.getInstance().getSecret();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(ctx.getToken())
                    .getBody();
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            if (subject == null || subject.isBlank() || role == null || role.isBlank()) {
                ctx.setError(401, "Invalid token");
                return;
            }
            ctx.setSubject(subject);
            ctx.setTokenRole(role);
            next(ctx);
        } catch (JwtException | IllegalArgumentException e) {
            ctx.setError(401, "Invalid token signature or expired");
        }
    }
}
