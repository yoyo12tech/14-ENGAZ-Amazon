package com.team14.amazon.shipping.config; // Adjust to your package structure

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // Grab the current incoming HTTP request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            // If the incoming request has a token, attach it to the outgoing Feign request
            if (authHeader != null) {
                requestTemplate.header(AUTHORIZATION_HEADER, authHeader);
            }
        }
    }
}