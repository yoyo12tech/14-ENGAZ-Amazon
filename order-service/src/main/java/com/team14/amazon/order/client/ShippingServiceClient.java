package com.team14.amazon.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@FeignClient(name = "shipping-service", url = "${feign.shipping-service.url}")
public interface ShippingServiceClient {
    @PutMapping("/api/shipments/order/{orderId}/delivered")
    int markShipmentDelivered(@PathVariable("orderId") Long orderId, @RequestParam("deliveredAt") String deliveredAt);
}
