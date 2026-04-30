package com.team14.amazon.shipping.security;

import com.team14.amazon.shipping.security.handler.AuthContext;
import com.team14.amazon.shipping.security.handler.AuthHandler;
import com.team14.amazon.shipping.security.handler.RoleAuthorizationHandler;
import com.team14.amazon.shipping.security.handler.SignatureValidationHandler;
import com.team14.amazon.shipping.security.handler.TokenExtractionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenExtractionHandler tokenExtractionHandler;
    private final SignatureValidationHandler signatureValidationHandler;
    private final RoleAuthorizationHandler roleAuthorizationHandler;

    public JwtAuthenticationFilter(TokenExtractionHandler tokenExtractionHandler,
                         SignatureValidationHandler signatureValidationHandler,
                         RoleAuthorizationHandler roleAuthorizationHandler) {
        this.tokenExtractionHandler = tokenExtractionHandler;
        this.signatureValidationHandler = signatureValidationHandler;
        this.roleAuthorizationHandler = roleAuthorizationHandler;

        // Wire the chain
        tokenExtractionHandler.setNext(signatureValidationHandler);
        signatureValidationHandler.setNext(roleAuthorizationHandler);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        AuthContext ctx = new AuthContext(request);
        tokenExtractionHandler.handle(ctx);

        if (ctx.hasError()) {
            response.setStatus(ctx.getErrorStatus());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"" + ctx.getErrorReason() + "\"}"
            );
            return;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + ctx.getTokenRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                ctx.getSubject(),
                null,
                Collections.singletonList(authority)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Attach claims to request for downstream use if needed
        request.setAttribute("uid", ctx.getUid());
        request.setAttribute("role", ctx.getTokenRole());
        request.setAttribute("subject", ctx.getSubject());

        filterChain.doFilter(request, response);
    }
}