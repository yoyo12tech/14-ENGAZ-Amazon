package com.team14.amazon.user.security;

import com.team14.amazon.user.repository.UserRepository;
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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Setup the chain
        AuthHandler head = new TokenExtractionHandler();
        head.setNext(new SignatureValidationHandler(jwtService))
            .setNext(new UserLoaderHandler(userRepository))
            .setNext(new RoleAuthorizationHandler());

        AuthContext ctx = new AuthContext(request, response);
        
        // Define required role
        String path = request.getRequestURI();
        if (path.matches("^/api/users/[^/]+/role$") && "PUT".equalsIgnoreCase(request.getMethod())) {
            ctx.setRequiredRole("ADMIN");
        } else {
            ctx.setRequiredRole("CUSTOMER"); // CUSTOMER is default and ADMIN can also access
        }

        head.handle(ctx);

        if (ctx.isAborted()) {
            return; // Short-circuit, response status is already set
        }

        // All checks passed, set SecurityContext
        if (ctx.getUser() != null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    ctx.getUser(), null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + ctx.getUser().getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/register") || 
               path.startsWith("/api/auth/login") ||
               path.endsWith("/health");
    }
}