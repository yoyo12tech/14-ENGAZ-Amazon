package com.team14.amazon.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "billing-service", url = "${feign.billing-service.url}")
public interface BillingServiceClient {

    @GetMapping("/api/transactions/user/{userId}/total")
    BigDecimal getUserTransactionTotal(
            @PathVariable("userId") Long userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    @GetMapping("/api/transactions/user/{userId}/order-count")
    long getUserOrderCount(
            @PathVariable("userId") Long userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );
}
