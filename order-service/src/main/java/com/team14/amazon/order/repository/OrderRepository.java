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






    //MS3
    // ── S1-F3: summary for a user ────────────────────────────────────────────────
    @Query(value = """
    SELECT COUNT(*) AS total_orders,
           SUM(CASE WHEN status IN ('DELIVERED','PAID') THEN 1 ELSE 0 END) AS completed_orders,
           SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END)           AS cancelled_orders,
           COALESCE(SUM(CASE WHEN status IN ('DELIVERED','PAID') THEN total_amount ELSE 0 END), 0) AS total_spent
    FROM orders WHERE user_id = :userId
    """, nativeQuery = true)
    Map<String, Object> getUserOrderSummaryRaw(@Param("userId") Long userId);

    // ── S1-F4: active order count ─────────────────────────────────────────────────
    @Query(value = """
    SELECT COUNT(*) FROM orders
    WHERE user_id = :userId
      AND status IN ('PENDING','CONFIRMED','SHIPPED','PAYMENT_PENDING')
    """, nativeQuery = true)
    int getActiveOrderCountForUser(@Param("userId") Long userId);

    // ── S1-F9: DELIVERED order count ──────────────────────────────────────────────
    @Query(value = """
    SELECT COUNT(*) FROM orders
    WHERE user_id = :userId AND status = 'DELIVERED'
    """, nativeQuery = true)
    long getDeliveredOrderCountForUser(@Param("userId") Long userId);

    // ── S2-F3: product sales aggregate ───────────────────────────────────────────
    @Query(value = """
    SELECT COALESCE(SUM(oi.quantity), 0)                             AS total_units_sold,
           COALESCE(SUM(oi.quantity * oi.price_at_purchase), 0)      AS total_revenue,
           COALESCE(AVG(oi.price_at_purchase), 0)                    AS avg_selling_price
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE oi.product_id = :productId
      AND CAST(o.status AS text) IN ('DELIVERED','PAID')
      AND o.delivered_at BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp)
    """, nativeQuery = true)
    Map<String, Object> getProductSalesRaw(@Param("productId") Long productId,
                                           @Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    // ── S2-F4: pending order count for a product ──────────────────────────────────
    @Query(value = """
    SELECT COUNT(*) FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE oi.product_id = :productId AND CAST(o.status AS text) = 'PENDING'
    """, nativeQuery = true)
    int getPendingOrderCountForProduct(@Param("productId") Long productId);

    // ── S2-F6: total units sold for a product ─────────────────────────────────────
    @Query(value = """
    SELECT COALESCE(SUM(oi.quantity), 0)
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE oi.product_id = :productId AND CAST(o.status AS text) IN ('DELIVERED','PAID')
    """, nativeQuery = true)
    long getUnitsSoldForProduct(@Param("productId") Long productId);

    // ── S2-F9: recent sales count (last N days) ───────────────────────────────────
    @Query(value = """
    SELECT COALESCE(SUM(oi.quantity), 0)
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE oi.product_id = :productId
      AND CAST(o.status AS text) IN ('DELIVERED','PAID')
      AND o.delivered_at >= CURRENT_DATE - CAST(:days AS INT)
    """, nativeQuery = true)
    int getRecentSalesCountForProduct(@Param("productId") Long productId,
                                      @Param("days") int days);

    // ── S2-F7/S2-F8: has user purchased product ───────────────────────────────────
    @Query(value = """
    SELECT COUNT(*) > 0
    FROM order_items oi
    JOIN orders o ON o.id = oi.order_id
    WHERE o.user_id = :userId
      AND oi.product_id = :productId
      AND CAST(o.status AS text) IN ('DELIVERED','PAID')
    """, nativeQuery = true)
    boolean hasUserPurchasedProduct(@Param("userId") Long userId,
                                    @Param("productId") Long productId);

// ── S5-F12: items for an order ────────────────────────────────────────────────
// (already returned by the JPA relationship — just need the controller endpoint)
// Use findById and getOrderItems() from the entity instead of a native query
}