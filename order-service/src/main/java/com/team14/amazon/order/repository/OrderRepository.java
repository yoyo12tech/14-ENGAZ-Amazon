package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :quantity WHERE id = :productId", nativeQuery = true)
    void restoreProductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}