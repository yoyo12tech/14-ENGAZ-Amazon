package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = "SELECT * FROM products WHERE id = :productId", nativeQuery = true)
    Map<String, Object> findProductById(@Param("productId") Long productId);



    @Query("SELECT COALESCE(MAX(i.itemOrder), 0) FROM OrderItem i WHERE i.order.id = :orderId")
    int findMaxItemOrderByOrderId(@Param("orderId") Long orderId);

    Optional<OrderItem> findByIdAndOrderId(Long itemId, Long orderId);


    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}