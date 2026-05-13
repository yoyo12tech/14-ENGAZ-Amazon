package com.team14.amazon.shipping.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "product-service", url = "${feign.product-service.url}")
public interface ProductServiceClient {
}
