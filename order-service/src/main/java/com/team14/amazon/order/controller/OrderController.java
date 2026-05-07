package com.team14.amazon.order.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team14.amazon.order.dto.*;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.service.OrderItemService;
import com.team14.amazon.order.service.OrderService;
import com.team14.amazon.order.service.RecommendationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final RecommendationService recommendationService;
    private static final Logger log =
            LoggerFactory.getLogger(OrderController.class);


    public OrderController(OrderService orderService, OrderItemService orderItemService,
                           RecommendationService recommendationService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.recommendationService = recommendationService;


    }


    @GetMapping("/analytics")
    public ResponseEntity<OrderAnalyticsDTO> getOrderAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(orderService.getOrderAnalytics(startDate, endDate));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Order>> searchOrders(
            @RequestParam(required = false) String status,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        try {
            List<Order> orders = orderService.search(status, startDate, endDate);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }


    @GetMapping("/metadata/search")
    public List<Order> searchOrdersByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return orderService.searchByMetadata(key, value);
    }
    @PostMapping("/estimate")
    public ResponseEntity<OrderEstimateDTO> getEstimate(@RequestBody List<Map<String, Object>> items) {
        return ResponseEntity.ok(orderService.estimateOrder(items));
    }

    @GetMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderItem> getOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId
    ) {
        OrderItem item = orderItemService.getOrderItem(orderId, itemId);
        return ResponseEntity.ok(item);
    }


    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<Void> deleteOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId
    ) {
        orderItemService.deleteOrderItem(orderId, itemId);
        return ResponseEntity.noContent().build(); // 204 ✅
    }

    @GetMapping("/{orderId}/details")
    public ResponseEntity<OrderDetailsDTO> getOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetails(orderId));
    }





    @PostMapping("/{orderId}/items")
    public ResponseEntity<Order> addItemsToOrder(@PathVariable Long orderId, @RequestBody List<OrderItemRequestDTO> items) {
        return ResponseEntity.ok(orderService.addItemsToOrder(orderId, items));
    }




    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.create(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id,
                                        @RequestBody Order order) {
        return ResponseEntity.ok(orderService.update(id, order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }






//    @PutMapping("/{id}/deliver")
//    public ResponseEntity<Order> markAsDelivered(
//            @PathVariable Long id,
//            @RequestBody DeliverRequest request) {
//        return ResponseEntity.ok(orderService.markAsDelivered(id, request.getMethod()));
//    }



    @PostMapping("/{orderId}/record-co-purchase")
    public ResponseEntity<?> recordCoPurchase(@PathVariable Long orderId) {

        try {
            orderService.recordCoPurchase(orderId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            throw e; // IMPORTANT: rethrow so Spring handles correct status
        }
    }

    //claude code
    @PutMapping("/{id}/deliver")
    public ResponseEntity<Order> markAsDelivered(
            @PathVariable Long id,
            @RequestBody(required = false) DeliverRequest request) {
        String method = (request != null && request.getMethod() != null)
                ? request.getMethod() : "CASH_ON_DELIVERY";
        return ResponseEntity.ok(orderService.markAsDelivered(id));
    }





      


    @PutMapping("/{orderId}/confirm")
    public Order confirmOrder(@PathVariable Long orderId,
                              @RequestParam Long shippingAddressId) {
        return orderService.confirmOrder(orderId, shippingAddressId);
    }



    //S3
    // ADD inside OrderController.java — after the existing getOrderAnalytics() method

    /**
     * S3-F10: GET /api/orders/analytics/dashboard
     *
     * DISTINCT from M1's GET /api/orders/analytics (S3-F6).
     * Both coexist — do NOT overwrite the existing /analytics endpoint.
     */
    @GetMapping("/analytics/dashboard")
    public ResponseEntity<?> getOrderAnalyticsDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 400 if startDate is after endDate
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("startDate must not be after endDate");
        }

        // Log ANALYTICS_VIEWED on EVERY call — outside the cached method
        // so it runs even on cache hits (spec §10.3.1 step d)
        orderService.logAnalyticsViewed(startDate, endDate);

        OrderAnalyticsDashboardDTO result = orderService.getOrderAnalyticsDashboard(startDate, endDate);
        return ResponseEntity.ok(result);
    }





    @GetMapping("/recommendations")
    public ResponseEntity<List<ProductRecommendationDTO>> getRecommendations(
            @RequestParam Long productId,
            @RequestParam(required = false, defaultValue = "5") int limit) {

        try {
            return ResponseEntity.ok(
                    recommendationService.getRecommendations(productId, limit)
            );
        } catch (Exception e) {
            log.warn("Recommendations unavailable", e);

            // graceful fallback instead of crashing endpoint
            return ResponseEntity.ok(List.of());
        }
    }
}