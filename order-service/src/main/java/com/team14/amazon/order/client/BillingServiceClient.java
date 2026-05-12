package com.team14.amazon.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "billing-service", url = "${feign.billing-service.url}")
public interface BillingServiceClient {
    @PostMapping("/api/transactions")
    int createTransaction(@RequestBody Map<String, Object> transactionRequest);
}
