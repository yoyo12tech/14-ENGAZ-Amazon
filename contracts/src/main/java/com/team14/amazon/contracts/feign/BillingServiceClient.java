package com.team14.amazon.contracts.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "billing-service", url = "${feign.billing-service.url}")
public interface BillingServiceClient {

    @GetMapping("/api/transactions/user/{userId}/total")
    BigDecimal getUserTransactionTotal(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );

    @GetMapping("/api/transactions/user/{userId}/order-count")
    long getUserOrderCount(@PathVariable Long userId,
                           @RequestParam String startDate,
                           @RequestParam String endDate);
}
