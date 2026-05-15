package com.team14.amazon.contracts.feign;

import com.team14.amazon.contracts.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service", url = "${feign.product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductDTO getProduct(@PathVariable Long id);

    @GetMapping("/api/products/{id}/exists")
    Map<String, Boolean> productExists(@PathVariable Long id);

    @GetMapping("/api/products/batch")
    List<ProductDTO> getProductsBatch(@RequestParam("ids") List<Long> ids);
}