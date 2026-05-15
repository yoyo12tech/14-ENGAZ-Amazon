package com.team14.amazon.billing.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BillingFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bad request calling downstream: " + methodKey);
            case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Resource not found in downstream service: " + methodKey);
            case 403 -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied in downstream service: " + methodKey);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
