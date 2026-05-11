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




    @Query(value = "SELECT price FROM products WHERE id = :productId", nativeQuery = true)
    Optional<Double> getProductPrice(@Param("productId") Long productId);


    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :quantity WHERE id = :productId", nativeQuery = true)
    void restoreProductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);


    @Query(value = "SELECT * FROM orders o WHERE o.metadata ->> :key = :value", nativeQuery = true)
    List<Order> findByMetadata(@Param("key") String key, @Param("value") String value);

    // =========================
    // Shipment update
    // =========================
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    @Query(value = """
        INSERT INTO transactions (order_id, user_id, amount, status, created_at, method)
        VALUES (:orderId, :userId, :amount, :status, :createdAt, :method)
    """, nativeQuery = true)
    int createTransaction(@Param("orderId") Long orderId,
                          @Param("userId") Long userId,
                          @Param("amount") BigDecimal amount,
                          @Param("status") String status,
                          @Param("createdAt") LocalDateTime createdAt,
                          @Param("method") String method);



//    @Query("SELECT o FROM Order o WHERE o.orderedAt BETWEEN :startDate AND :endDate")
//    List<Order> findOrdersBetween(@Param("startDate") LocalDateTime startDate,
//                                  @Param("endDate") LocalDateTime endDate);



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
    @Query(value = "SELECT price FROM products WHERE id = :productId", nativeQuery = true)
    Double findProductPriceById(@Param("productId") Long productId);


    //old code
    @Query(value = "SELECT COUNT(*) > 0 FROM shipping_addresses WHERE id = :addressId", nativeQuery = true)
    boolean existsShippingAddressById(@Param("addressId") Long addressId);


    @Query(value = "SELECT * FROM products WHERE id = :productId", nativeQuery = true)
    Map<String, Object> findProductById(@Param("productId") Long productId);

    @Modifying
    @Query(
            value = "UPDATE products SET stock_quantity = stock_quantity - :qty WHERE id = :id AND stock_quantity >= :qty",
            nativeQuery = true
    )
    int updateProductStock(@Param("id") Long id, @Param("qty") Integer qty);










    //MS2
    // ADD these methods to your existing OrderRepository interface
// File: src/main/java/com/team14/amazon/order/repository/OrderRepository.java

// ── S3-F10 aggregation queries ────────────────────────────────────────────────

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
}



