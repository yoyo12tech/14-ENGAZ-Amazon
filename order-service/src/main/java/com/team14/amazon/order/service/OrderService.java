package com.team14.amazon.order.service;

import com.team14.amazon.order.dto.OrderEstimateDTO;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
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



    public OrderEstimateDTO estimateOrder(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request must contain at least one item");
        }

        int totalItemCount = 0;
        double subtotal = 0.0;

        for (Map<String, Object> item : items) {
            Long productId = ((Number) item.get("productId")).longValue();
            Integer quantity = ((Number) item.get("quantity")).intValue();

            // Get price directly from repository
            Double price = orderRepository.findProductPriceById(productId);
            if (price == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
            }

            totalItemCount += quantity;
            subtotal += price * quantity;
        }

        // Apply discount tiers
        double discountApplied = 0.0;
        if (totalItemCount >= 6 && totalItemCount <= 15) discountApplied = 5.0;
        else if (totalItemCount > 15) discountApplied = 10.0;

        // Shipping cost
        double shippingCost = subtotal < 500 ? 50.0 : 0.0;

        // Final estimated total
        double estimatedTotal = subtotal * (1 - discountApplied / 100) + shippingCost;

        return new OrderEstimateDTO(totalItemCount, subtotal, shippingCost, estimatedTotal, discountApplied);
    }


    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}