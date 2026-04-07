package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    @Query(value = "SELECT price FROM products WHERE id = :productId", nativeQuery = true)
    Double findProductPriceById(@Param("productId") Long productId);

}