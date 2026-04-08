package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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


    @Transactional
    public Order markAsDelivered(Long id, String method) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order already delivered");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must be SHIPPED to deliver");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(now);

        orderRepository.markShipmentDelivered(id, now);

        BigDecimal amount = BigDecimal.valueOf(order.getTotalAmount());

        orderRepository.createTransaction(
                id,
                order.getUserId(),
                amount,
                "PENDING",
                now,
                method // dynamic from body takes "CREDIT_CARD|CASH_ON_DELIVERY|WALLET", in the body of the postman req
        );

        return orderRepository.save(order);
    }

}