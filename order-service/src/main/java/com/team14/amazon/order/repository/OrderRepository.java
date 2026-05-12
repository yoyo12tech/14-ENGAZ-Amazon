package com.team14.amazon.order.repository;


import java.util.Optional;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;






@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = "SELECT * FROM orders o WHERE o.metadata ->> :key = :value", nativeQuery = true)
    List<Order> findByMetadata(@Param("key") String key, @Param("value") String value);

    List<Order> findByOrderedAtBetweenAndStatusOrderByOrderedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus status
    );

    List<Order> findByOrderedAtBetweenOrderByOrderedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderedAt BETWEEN :start AND :end")
    Long countOrdersBetween(@Param("start") LocalDateTime start,
                            @Param("end")   LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o " +
            "WHERE o.orderedAt BETWEEN :start AND :end AND o.totalAmount IS NOT NULL")
    Double sumTotalRevenueBetween(@Param("start") LocalDateTime start,
                                  @Param("end")   LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.orderedAt BETWEEN :start AND :end AND o.status = com.team14.amazon.order.model.OrderStatus.DELIVERED")
    Long countDeliveredBetween(@Param("start") LocalDateTime start,
                               @Param("end")   LocalDateTime end);

    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.orderedAt BETWEEN :start AND :end GROUP BY o.status")
    List<Object[]> countGroupByStatusBetween(@Param("start") LocalDateTime start,
                                             @Param("end")   LocalDateTime end);

    // M3: New methods for user-service integration
    @Query(value = """
    SELECT
        COUNT(o.id)                                                         AS totalOrders,
        COUNT(CASE WHEN o.status = 'DELIVERED'  THEN 1 END)                AS completedOrders,
        COUNT(CASE WHEN o.status = 'CANCELLED'  THEN 1 END)                AS cancelledOrders,
        COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.total_amount END), 0) AS totalSpent,
        COALESCE(AVG(CASE WHEN o.status = 'DELIVERED' THEN o.total_amount END), 0) AS averageOrderValue
    FROM orders o
    WHERE o.user_id = :userId
    """, nativeQuery = true)
    Object[] getOrderSummaryByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status NOT IN (com.team14.amazon.order.model.OrderStatus.DELIVERED, com.team14.amazon.order.model.OrderStatus.CANCELLED, com.team14.amazon.order.model.OrderStatus.RETURNED)")
    int countActiveOrdersForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    long countAllOrdersForUser(@Param("userId") Long userId);
}



