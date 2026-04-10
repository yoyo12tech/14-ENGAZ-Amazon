package com.team14.amazon.order.controller;


import com.team14.amazon.order.DTO.OrderItemRequestDTO;
import com.team14.amazon.order.DTO.OrderAnalyticsDTO;
import com.team14.amazon.order.DTO.DeliverRequest;
import com.team14.amazon.order.DTO.OrderEstimateDTO;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;

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

    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
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
    @GetMapping("/analytics")
    public ResponseEntity<OrderAnalyticsDTO> getOrderAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(orderService.getOrderAnalytics(startDate, endDate));
    }


    @GetMapping("/metadata/search")
    public List<Order> searchOrdersByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return orderService.searchByMetadata(key, value);
    }


    @PutMapping("/{id}/deliver")
    public ResponseEntity<Order> markAsDelivered(
            @PathVariable Long id,
            @RequestBody DeliverRequest request) {
        return ResponseEntity.ok(orderService.markAsDelivered(id, request.getMethod()));
    }


    @GetMapping("/search")
    public List<Order> searchOrders(
            @RequestParam(required = false) String status,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        return orderService.search(status, startDate, endDate);
    }

      
    @PostMapping("/estimate")
    public ResponseEntity<OrderEstimateDTO> getEstimate(@RequestBody List<Map<String, Object>> items) {
        return ResponseEntity.ok(orderService.estimateOrder(items));
    }

    @PutMapping("/{orderId}/confirm")
    public Order confirmOrder(@PathVariable Long orderId,
                              @RequestParam Long shippingAddressId) {
        return orderService.confirmOrder(orderId, shippingAddressId);
    }
}