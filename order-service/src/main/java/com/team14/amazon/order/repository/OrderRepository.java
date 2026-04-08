package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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