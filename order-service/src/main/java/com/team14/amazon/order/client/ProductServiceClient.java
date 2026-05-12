package com.team14.amazon.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "product-service", url = "${feign.product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}/price")
    Double getProductPrice(@PathVariable("id") Long id);

    @GetMapping("/api/products/{id}/details")
    Map<String, Object> getProductById(@PathVariable("id") Long id);

    @PutMapping("/api/products/{id}/stock/deduct")
    int updateProductStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/products/{id}/stock/restore")
    void restoreProductStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}
