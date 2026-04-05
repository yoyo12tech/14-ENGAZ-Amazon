package com.team14.amazon.product.repository;

import com.team14.amazon.product.model.ProductReview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByUserId(Long userId);
}