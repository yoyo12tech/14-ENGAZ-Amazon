package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :quantity WHERE id = :productId", nativeQuery = true)
    void restoreProductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);


    @Query(value = "SELECT * FROM orders o WHERE o.metadata ->> :key = :value", nativeQuery = true)
    List<Order> findByMetadata(@Param("key") String key, @Param("value") String value);

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



    @Query("SELECT o FROM Order o WHERE o.orderedAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetween(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);



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

    @Query(value = "SELECT COUNT(*) > 0 FROM shipments WHERE id = :addressId", nativeQuery = true)
    boolean existsShippingAddressById(@Param("addressId") Long addressId);

    @Query(value = "SELECT * FROM products WHERE id = :productId", nativeQuery = true)
    Map<String, Object> findProductById(@Param("productId") Long productId);

    @Modifying
    @Query(
            value = "UPDATE products SET stock_quantity = stock_quantity - :qty WHERE id = :id AND stock_quantity >= :qty",
            nativeQuery = true
    )
    int updateProductStock(@Param("id") Long id, @Param("qty") Integer qty);
}



