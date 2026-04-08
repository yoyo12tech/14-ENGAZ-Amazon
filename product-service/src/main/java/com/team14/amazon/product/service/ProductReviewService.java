package com.team14.amazon.product.service;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductReview;
import com.team14.amazon.product.repository.ProductRepository;
import com.team14.amazon.product.repository.ProductReviewRepository;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ProductReviewService(ProductReviewRepository reviewRepository,
                                ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public List<ProductReview> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Optional<ProductReview> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public ProductReview createReview(ProductReview review) {
        return reviewRepository.save(review);
    }

    public Optional<ProductReview> updateReview(Long id, ProductReview updatedReview) {
        return reviewRepository.findById(id).map(existing -> {
            existing.setRating(updatedReview.getRating());
            existing.setTitle(updatedReview.getTitle());
            existing.setComment(updatedReview.getComment());
            existing.setVerified(updatedReview.getVerified());
            existing.setMetadata(updatedReview.getMetadata());
            return reviewRepository.save(existing);
        });
    }

    public boolean deleteReview(Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ProductReview> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    // ─── S2-F7: Add Product Review with Rating Recalculation (Transactional) ────

    @Transactional
    public Product addReviewWithRatingRecalculation(Long productId, ProductReview review) {

        // 1. Find product — 404 if not found
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // 2. Verify user exists — 404 if not found
        boolean userExists = reviewRepository.existsUserById(review.getUserId());
        if (!userExists) {
            throw new ResourceNotFoundException("User not found with id: " + review.getUserId());
        }

        // 3. Verify user has a DELIVERED order containing this product — 400 if not
        boolean hasPurchased = reviewRepository.userHasDeliveredOrderForProduct(
                review.getUserId(), productId);
        if (!hasPurchased) {
            throw new IllegalArgumentException("User must purchase the product before reviewing it.");
        }

        // 4. Validate rating 1–5 — 400 if invalid
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        // 5. Create and save the review
        review.setProduct(product);
        reviewRepository.save(review);

        // 6. Recalculate running average rating on product
        int newTotalRatings = product.getTotalRatings() + 1;
        double newRating = ((product.getRating() * product.getTotalRatings()) + review.getRating())
                / newTotalRatings;

        product.setTotalRatings(newTotalRatings);
        product.setRating(newRating);

        // 7. Save updated product and return it
        return productRepository.save(product);
    }
}