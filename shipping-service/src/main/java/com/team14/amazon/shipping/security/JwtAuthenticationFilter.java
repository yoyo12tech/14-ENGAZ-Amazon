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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

                String path = request.getServletPath();

                if (HttpMethod.OPTIONS.matches(request.getMethod())
                        || path.contains("/health")
                        || "/error".equals(path)
                        || path.contains("/actuator")) {
                
                    filterChain.doFilter(request, response);
                    return;
                }
        AuthContext ctx = new AuthContext(request);

        AuthHandler tokenExtraction = new TokenExtractionHandler();
        AuthHandler signatureValidation = new SignatureValidationHandler();
        AuthHandler roleAuthorization = new RoleAuthorizationHandler();

        tokenExtraction.setNext(signatureValidation);
        signatureValidation.setNext(roleAuthorization);

        tokenExtraction.handle(ctx);

        if (ctx.hasError()) {
            response.setStatus(ctx.getErrorStatus());
            response.getWriter().write(ctx.getErrorReason());
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                ctx.getSubject(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + ctx.getTokenRole()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        filterChain.doFilter(request, response);
    }
}
