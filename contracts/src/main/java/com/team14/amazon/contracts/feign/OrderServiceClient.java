package com.team14.amazon.contracts.feign;

import com.team14.amazon.contracts.dto.OrderDTO;
import com.team14.amazon.contracts.dto.OrderItemDTO;
import com.team14.amazon.contracts.dto.OrderDetailsDTO;
import com.team14.amazon.contracts.dto.OrderSummaryDTO;
import com.team14.amazon.contracts.dto.ProductSalesAggregateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {


    //Checked and tested
    @GetMapping("/api/orders/{orderId}")
    OrderDTO getOrder(@PathVariable("orderId") Long orderId);


    //The below is likely an extra feign endPoint
    @GetMapping("/api/orders/{orderId}/details")
    OrderDetailsDTO getOrderDetails(@PathVariable("orderId") Long orderId);



    //checked and tested
    @GetMapping("/api/orders/user/{userId}/summary")
    OrderSummaryDTO getUserOrderSummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/user/{userId}/active-count")
    int getActiveOrderCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/user/{userId}/count")
    long getTotalOrderCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/orders/product/{productId}/sales")
    ProductSalesAggregateDTO getProductSales(
            @PathVariable("productId") Long productId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    @GetMapping("/api/orders/product/{productId}/pending-count")
    int getPendingOrderCountForProduct(@PathVariable("productId") Long productId);

    @GetMapping("/api/orders/product/{productId}/units-sold")
    long getUnitsSold(@PathVariable("productId") Long productId);

    @GetMapping("/api/orders/user/{userId}/has-purchased/{productId}")
    boolean hasUserPurchasedProduct(@PathVariable("userId") Long userId,
                                    @PathVariable("productId") Long productId);

    @GetMapping("/api/orders/product/{productId}/recent-sales-count")
    int getRecentSalesCount(@PathVariable("productId") Long productId,
                            @RequestParam("days") int days);


    @GetMapping("/api/orders/{orderId}/items")
    List<OrderItemDTO> getOrderItems(@PathVariable("orderId") Long orderId);
}