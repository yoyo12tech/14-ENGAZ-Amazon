package com.team14.amazon.shipping.client;

import com.team14.amazon.shipping.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{orderId}")
    OrderDTO getOrder(@PathVariable Long orderId);
}
