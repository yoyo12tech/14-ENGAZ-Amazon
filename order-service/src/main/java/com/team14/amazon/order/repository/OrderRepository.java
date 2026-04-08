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
        UPDATE shipments
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
        INSERT INTO transactions (order_id, user_id, amount, status, created_at, method)
        VALUES (:orderId, :userId, :amount, CAST(:status AS transaction_status), :createdAt, CAST(:method AS transaction_method))
    """, nativeQuery = true)
    int createTransaction(@Param("orderId") Long orderId,
                          @Param("userId") Long userId,
                          @Param("amount") BigDecimal amount,
                          @Param("status") String status,
                          @Param("createdAt") LocalDateTime createdAt,
                          @Param("method") String method);
}
