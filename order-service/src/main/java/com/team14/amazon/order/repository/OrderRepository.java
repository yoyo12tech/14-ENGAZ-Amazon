package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // =========================
    // Shipment update
    // =========================
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE shipment
        SET status = 'DELIVERED',
            actual_delivery = :deliveredAt
        WHERE order_id = :orderId
    """, nativeQuery = true)
    int markShipmentDelivered(@Param("orderId") Long orderId,
                              @Param("deliveredAt") LocalDateTime deliveredAt);

    // =========================
    // Transaction insert
    // =========================
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO transaction (order_id, amount, status, created_at)
        VALUES (:orderId, :amount, :status, :createdAt)
    """, nativeQuery = true)
    int createTransaction(@Param("orderId") Long orderId,
                          @Param("amount") BigDecimal amount,
                          @Param("status") String status,
                          @Param("createdAt") LocalDateTime createdAt);
}