package com.team14.amazon.shipping.security.handler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class SignatureValidationHandler extends AuthHandler {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public void handle(AuthContext ctx) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            SecretKey key = Keys.hmacShaKeyFor(decodedKey);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(ctx.getToken())
                    .getBody();

            String subject = claims.getSubject();
            Long uid = claims.get("uid", Long.class);
            String role = claims.get("role", String.class);

            if (subject == null || subject.isBlank() || role == null || role.isBlank() || uid == null) {
                ctx.setError(401, "Invalid token: Missing required claims");
                return;
            }

            ctx.setSubject(subject);
            ctx.setTokenRole(role);
            ctx.setUid(uid);

            next(ctx);

        } catch (JwtException | IllegalArgumentException e) {
            ctx.setError(401, "Invalid token signature or expired");
            e.printStackTrace();
        }
    }
}