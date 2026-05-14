package com.team14.amazon.shipping.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{orderId}")
    Object getOrder(@PathVariable("orderId") Long orderId);
}