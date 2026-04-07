package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

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

    @Transactional(readOnly = true)
    public List<Order> searchByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must be provided");
        }
        return orderRepository.findByMetadata(key, value);
    }

}