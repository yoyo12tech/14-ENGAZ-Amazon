package com.team14.amazon.contracts.feign;


import com.team14.amazon.contracts.dto.OrderDTO;
import com.team14.amazon.contracts.dto.OrderSummaryDTO;
import com.team14.amazon.contracts.dto.ProductSalesAggregateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {


    @GetMapping("/api/orders/{orderId}")
    OrderDTO getOrder(@PathVariable Long orderId);

    @GetMapping("/api/orders/user/{userId}/summary")
    OrderSummaryDTO getUserOrderSummary(@PathVariable Long userId);


    @GetMapping("/api/orders/user/{userId}/active-count")
    int getActiveOrderCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/user/{userId}/count")
    long getTotalOrderCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/product/{productId}/sales")
    ProductSalesAggregateDTO getProductSales(
            @PathVariable Long productId,
            @RequestParam String startDate,
            @RequestParam String endDate
    );

    @GetMapping("/api/orders/product/{productId}/pending-count")
    int getPendingOrderCountForProduct(@PathVariable Long productId);


    @GetMapping("/api/orders/product/{productId}/units-sold")
    long getUnitsSold(@PathVariable Long productId);

    @GetMapping("/api/orders/user/{userId}/has-purchased/{productId}")
    boolean hasUserPurchasedProduct(@PathVariable Long userId,
                                    @PathVariable Long productId);


    @GetMapping("/api/orders/product/{productId}/recent-sales-count")
    int getRecentSalesCount(@PathVariable Long productId,
                            @RequestParam("days") int days);

    @GetMapping("/api/orders/product/{productId}/units-sold")
    long getUnitsSoldForProduct(@PathVariable Long productId);

}
