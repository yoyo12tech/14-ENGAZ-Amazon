package com.team14.amazon.user.client;

import com.team14.amazon.user.dto.OrderSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {

    @GetMapping("/api/orders/user/{userId}/summary")
    OrderSummaryDTO getUserOrderSummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/user/{userId}/active-count")
    int getActiveOrderCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/user/{userId}/count")
    long getTotalOrderCount(@PathVariable("userId") Long userId);

}
