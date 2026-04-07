package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {



    List<Order> findByOrderedAtBetweenAndStatusOrderByOrderedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus status
    );

    // Status is not provided
    List<Order> findByOrderedAtBetweenOrderByOrderedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}