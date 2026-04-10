package com.team14.amazon.product.repository;

import com.team14.amazon.product.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    Optional<ProductReview> findByIdAndProductId(Long id, Long productId);

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

    // S2-F8: Verify that a given userId has the ADMIN role (cross-service native SQL)
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId AND role = 'ADMIN'",
            nativeQuery = true)
    boolean isAdminUser(@Param("userId") Long userId);

    // S2-F9: Check if a product has any review with rating <= 2
    @Query("SELECT COUNT(r) > 0 FROM ProductReview r " +
            "WHERE r.product.id = :productId AND r.rating <= 2")
    boolean hasNegativeReviews(@Param("productId") Long productId);
}