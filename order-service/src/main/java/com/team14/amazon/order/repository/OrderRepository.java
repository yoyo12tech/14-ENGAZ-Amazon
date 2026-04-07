package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.OrderStatus; // You'll need to import this
import com.team14.amazon.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByStatusAndOrderItemsProductId(OrderStatus status, Long productId);
}