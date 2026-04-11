package com.team14.amazon.order.controller;

import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.service.OrderItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @PostMapping
    public ResponseEntity<OrderItem> create(@RequestBody OrderItem item) {
        return ResponseEntity.ok(orderItemService.create(item.getOrder().getId(), item));
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<OrderItem> create(@PathVariable Long orderId,
                                            @RequestBody OrderItem item) {
        return ResponseEntity.ok(orderItemService.create(orderId, item));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderItem> getById(@PathVariable Long id) {
        return orderItemService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderItem>> getAll() {
        return ResponseEntity.ok(orderItemService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderItem> update(@PathVariable Long id,
                                            @RequestBody OrderItem item) {
        return ResponseEntity.ok(orderItemService.update(id, item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}