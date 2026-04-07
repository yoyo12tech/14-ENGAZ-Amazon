package com.team14.amazon.order.service;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Order markAsDelivered(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Order already delivered");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Order must be SHIPPED to deliver");
        }

        // Update order
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

//          I am not sure if this works but for now till the shipment and billing branches are pushed OwO
//        // Update shipment
//        Shipment shipment = shipmentRepository.findByOrderId(id)
//                .orElseThrow(() -> new RuntimeException("Shipment not found"));
//        shipment.setStatus(ShipmentStatus.DELIVERED);
//        shipment.setActualDelivery(LocalDateTime.now());
//
//        // Create transaction
//        Transaction tx = new Transaction();
//        tx.setOrderId(id);
//        tx.setAmount(order.getTotalAmount());
//        tx.setStatus(TransactionStatus.PENDING);
//        tx.setCreatedAt(LocalDateTime.now());
//        transactionRepository.save(tx);

        return orderRepository.save(order);
    }
}