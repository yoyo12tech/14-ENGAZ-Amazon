package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.repository.OrderItemRepository;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public OrderItemService(OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    public OrderItem create(Long orderId, OrderItem item) {
        // Must set owner side of relationship (Lab 4 slide 22)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        item.setOrder(order);
        return orderItemRepository.save(item);
    }

    public Optional<OrderItem> getById(Long id) {
        return orderItemRepository.findById(id);
    }

    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }

    @Transactional // Ensures the update is atomic
    public OrderItem update(Long id, OrderItem updated) {
        // 1. Fetch the original record
        OrderItem existing = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderItem not found: " + id));

        // 2. Only update if the value is provided (Null-Safety)
        if (updated.getQuantity() != null) {
            existing.setQuantity(updated.getQuantity());
        }

        if (updated.getMetadata() != null) {
            existing.setMetadata(updated.getMetadata());
        }

        if (updated.getItemOrder() != null) {
            existing.setItemOrder(updated.getItemOrder());
        }
        if (updated.getPriceAtPurchase() != null) {
            existing.setPriceAtPurchase(updated.getPriceAtPurchase());
        }
        return orderItemRepository.save(existing);
    }
    public void delete(Long id) {
        orderItemRepository.deleteById(id);
    }
}