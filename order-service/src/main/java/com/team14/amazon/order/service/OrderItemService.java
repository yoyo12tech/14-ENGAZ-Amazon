package com.team14.amazon.order.service;

import com.team14.amazon.order.DTO.OrderItemRequestDTO;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.repository.OrderItemRepository;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
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

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId
                ));

        Map<String, Object> product = orderRepository.findProductById(item.getProductId());

        if (product == null || product.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found: " + item.getProductId()
            );
        }

        item.setOrder(order);

        item.setPriceAtPurchase(
                ((Number) product.get("price")).doubleValue()
        );

        return orderItemRepository.save(item);
    }




    public OrderItem getOrderItem(Long orderId, Long itemId) {

        return orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order item not found"
                ));
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


    public int findMaxItemOrderByOrderId2( Long orderId)
    {
        return orderItemRepository.findMaxItemOrderByOrderId(orderId);
    }




    public void delete(Long id) {
        orderItemRepository.deleteById(id);
    }

    public void deleteOrderItem(Long orderId, Long itemId) {

        OrderItem item = orderItemRepository
                .findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order item not found"
                ));

        orderItemRepository.delete(item);
    }
}