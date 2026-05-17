package com.team14.amazon.product.config;

import com.team14.amazon.product.filter.CorrelationIdFilter;
import com.team14.amazon.product.filter.LoggingFilter;
import com.team14.amazon.product.security.JwtAuthenticationFilter;
import com.team14.amazon.product.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final CorrelationIdFilter correlationIdFilter;
    private final LoggingFilter loggingFilter;

    public SecurityConfig(JwtService jwtService, CorrelationIdFilter correlationIdFilter, LoggingFilter loggingFilter) {
        this.jwtService = jwtService;
        this.correlationIdFilter = correlationIdFilter;
        this.loggingFilter = loggingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/products/health", "/error", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loggingFilter, CorrelationIdFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CorrelationIdFilter.class);
        return http.build();
    }
}

