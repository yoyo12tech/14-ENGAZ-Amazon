package com.team14.amazon.product.security;

import com.team14.amazon.product.security.handler.AuthContext;
import com.team14.amazon.product.security.handler.AuthHandler;
import com.team14.amazon.product.security.handler.RoleAuthorizationHandler;
import com.team14.amazon.product.security.handler.SignatureValidationHandler;
import com.team14.amazon.product.security.handler.TokenExtractionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if (HttpMethod.OPTIONS.matches(request.getMethod()) || path.contains("/health") || "/error".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthContext ctx = new AuthContext(request);
        AuthHandler tokenExtraction = new TokenExtractionHandler();
        AuthHandler signatureValidation = new SignatureValidationHandler(jwtService);
        AuthHandler roleAuthorization = new RoleAuthorizationHandler();

        tokenExtraction.setNext(signatureValidation);
        signatureValidation.setNext(roleAuthorization);

        tokenExtraction.handle(ctx);

        if (ctx.hasError()) {
            response.setStatus(ctx.getErrorStatus());
            response.getWriter().write(ctx.getErrorReason());
            return;
        }

        if (ctx.getUserId() != null) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    ctx.getUserId(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + ctx.getRole()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}

