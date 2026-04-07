package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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


    public List<Order> search(String status, String startDateStr, String endDateStr) {

        // Define strict date format
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"
        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(startDateStr, formatter);
            endDate = LocalDate.parse(endDateStr, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD", e);
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        // Inclusive date range
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        if (status == null || status.isBlank()) {
            return orderRepository.findByOrderedAtBetweenOrderByOrderedAtDesc(
                    startDateTime,
                    endDateTime
            );
        } else {

            OrderStatus orderStatus = OrderStatus.fromString(status);
            return orderRepository.findByOrderedAtBetweenAndStatusOrderByOrderedAtDesc(
                    startDateTime,
                    endDateTime,
                    orderStatus
            );
        }
    }
    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}