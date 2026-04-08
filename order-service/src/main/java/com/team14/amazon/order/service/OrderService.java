package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING or CONFIRMED orders can be cancelled");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getOrderItems()) {
                orderRepository.restoreProductStock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order create(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> getById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Order update(Long id, Order updated) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        existing.setUserId(updated.getUserId());
        existing.setShippingAddressId(updated.getShippingAddressId());
        existing.setStatus(updated.getStatus());
        existing.setTotalAmount(updated.getTotalAmount());
        existing.setMetadata(updated.getMetadata());
        existing.setDeliveredAt(updated.getDeliveredAt());
        return orderRepository.save(existing);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}