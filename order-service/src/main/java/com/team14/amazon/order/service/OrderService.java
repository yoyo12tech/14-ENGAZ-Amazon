package com.team14.amazon.order.service;


import com.team14.amazon.order.DTO.OrderItemRequestDTO;
import com.team14.amazon.order.DTO.OrderAnalyticsDTO;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.DTO.OrderEstimateDTO;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.List;
@Service
public class OrderService {

    @Transactional
    public Order addItemsToOrder(Long orderId, List<OrderItemRequestDTO> itemsDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add items to non-pending orders");
        }

        int currentMaxItemOrder = order.getOrderItems().stream()
                .mapToInt(OrderItem::getItemOrder)
                .max()
                .orElse(0);

        for (OrderItemRequestDTO dto : itemsDto) {
            Double price = orderRepository.getProductPrice(dto.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + dto.productId()));

            currentMaxItemOrder++;

            OrderItem newItem = new OrderItem();
            newItem.setProductId(dto.productId());
            newItem.setQuantity(dto.quantity());
            newItem.setPriceAtPurchase(price);
            newItem.setItemOrder(currentMaxItemOrder);
            newItem.setMetadata(dto.metadata());
            newItem.setOrder(order);

            order.getOrderItems().add(newItem);
        }

        return orderRepository.save(order);
    }
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

    @Transactional(readOnly = true)
    public OrderAnalyticsDTO getOrderAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findOrdersBetween(start, end);

        long totalOrders = orders.size();
        long deliveredOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        double totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getTotalAmount)
                .sum();
        double averageOrderValue = deliveredOrders > 0 ? totalRevenue / deliveredOrders : 0.0;
        double completionRate = totalOrders > 0 ? (deliveredOrders * 100.0) / totalOrders : 0.0;

        return new OrderAnalyticsDTO(totalOrders, deliveredOrders, cancelledOrders,
                totalRevenue, averageOrderValue, completionRate);
    }




    public List<Order> searchByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must be provided");
        }
        return orderRepository.findByMetadata(key, value);
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



