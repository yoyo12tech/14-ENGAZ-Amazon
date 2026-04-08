package com.team14.amazon.product.repository;

import com.team14.amazon.product.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByUserId(Long userId);

    // S2-F7: Verify the user exists in the shared users table (cross-service native SQL)
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    // S2-F7: Verify user has a DELIVERED order containing this product (cross-service native SQL)
    @Query(value = """
            SELECT COUNT(*) > 0
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.user_id   = :userId
              AND oi.product_id = :productId
              AND o.status      = 'DELIVERED'
            """, nativeQuery = true)
    boolean userHasDeliveredOrderForProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId);
}