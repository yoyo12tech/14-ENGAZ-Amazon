package com.team14.amazon.product.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (path.contains("/health") || "/error".equals(path) || path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Received {} {}", method, path);

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("Returning {} for {} {}", response.getStatus(), method, path);
        }
    }
}
