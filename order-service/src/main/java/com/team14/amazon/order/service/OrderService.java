package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }


    @Transactional
    public Order confirmOrder(Long orderId, Long shippingAddressId) {
        // Find order with items
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not pending");
        }

        // Check shipping address exists
        if (!orderRepository.existsShippingAddressById(shippingAddressId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping address not found");
        }

        double totalAmount = 0.0;

        //Im using the virtual memory, this will work correct as long
        //as i update the virtual in memory when i save an orderItem
        for (OrderItem item : order.getOrderItems()) {
            // Use repository to check product existence and stock
            Map<String, Object> product = orderRepository.findProductById(item.getProductId());
            if (product == null || product.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + item.getProductId());
            }

            Integer stockQuantity = (Integer) product.get("stock_quantity");
            if (stockQuantity == null || stockQuantity < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + item.getProductId());
            }

            // Deduct stock via update query, check rows affected
            int updated = orderRepository.updateProductStock(item.getProductId(), (Integer)item.getQuantity());
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to deduct stock (insufficient) for product: " + item.getProductId());
            }

            totalAmount += item.getQuantity() * item.getPriceAtPurchase();
        }

        order.setShippingAddressId(shippingAddressId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(totalAmount);

        return orderRepository.save(order);
    }
}



