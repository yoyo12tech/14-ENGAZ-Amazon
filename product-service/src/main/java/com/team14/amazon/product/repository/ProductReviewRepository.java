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



    // S2-F9: Check if a product has any review with rating <= 2
    @Query("SELECT COUNT(r) > 0 FROM ProductReview r " +
            "WHERE r.product.id = :productId AND r.rating <= 2")
    boolean hasNegativeReviews(@Param("productId") Long productId);
}