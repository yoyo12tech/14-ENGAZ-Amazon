package com.team14.amazon.product.service;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductReview;
import com.team14.amazon.product.observer.EntityObserver;
import com.team14.amazon.product.repository.ProductRepository;
import com.team14.amazon.product.repository.ProductReviewRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.team14.amazon.product.dto.VerifyReviewRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final EntityObserver mongoEventLogger;

    public ProductReviewService(ProductReviewRepository reviewRepository,
                                ProductRepository productRepository,
                                @Qualifier("mongoEventLogger") EntityObserver mongoEventLogger) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.mongoEventLogger = mongoEventLogger;
    }

    private Map<String, Object> reviewPayload(Long productId, Long reviewId, Long userId, Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("timestamp", LocalDateTime.now());

        Map<String, Object> mergedDetails = new HashMap<>();
        if (details != null) {
            mergedDetails.putAll(details);
        }
        mergedDetails.put("reviewId", reviewId);
        mergedDetails.put("userId", userId);
        mergedDetails.put("source", "REVIEW");
        payload.put("details", mergedDetails);
        return payload;
    }

    public List<ProductReview> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Optional<ProductReview> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public Optional<ProductReview> getReviewByProductAndId(Long productId, Long reviewId) {
        return reviewRepository.findByIdAndProductId(reviewId, productId);
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
    public ProductReview addReviewWithRatingRecalculation(Long productId, ProductReview review) {

        // 1. Find product — 404 if not found
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // 2. Verify user exists — 400 if not found
        boolean userExists = reviewRepository.existsUserById(review.getUserId());
        if (!userExists) {
            throw new IllegalArgumentException("User not found with id: " + review.getUserId());
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
        ProductReview savedReview = reviewRepository.saveAndFlush(review);

        // 6. Recalculate running average rating on product
        int newTotalRatings = product.getTotalRatings() + 1;
        double newRating = ((product.getRating() * product.getTotalRatings()) + review.getRating())
                / (double) newTotalRatings;

        product.setTotalRatings(newTotalRatings);
        product.setRating(newRating);

        // 7. Save updated product and return the created review payload
        productRepository.save(product);

        mongoEventLogger.onEvent("REVIEW_ADDED", reviewPayload(
                productId,
                savedReview.getId(),
                savedReview.getUserId(),
                Map.of("rating", savedReview.getRating())
        ));

        return savedReview;
    }

    // ─── S2-F8: Verify Product Review (Transactional + Relationship) ─────────────

    @Transactional
    public Product verifyReview(Long productId, Long reviewId, VerifyReviewRequest request) {

        // 1. Ensure product exists — 404 if not found
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));

        // 2. Find review — 404 if not found
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + reviewId));

        // 3. Verify the review belongs to this product — 400 if not
        if (!review.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException(
                    "Review " + reviewId + " does not belong to product " + productId);
        }

        // 4. Verify the reviewer actually purchased this product — 400 if not
        boolean hasPurchased = reviewRepository.userHasDeliveredOrderForProduct(
                review.getUserId(), productId);
        if (!hasPurchased) {
            throw new IllegalArgumentException(
                    "Reviewer (userId=" + review.getUserId() +
                            ") has no delivered order for this product.");
        }

        // 5. Verify the verifiedBy user is an ADMIN — 403 if not
        boolean isAdmin = reviewRepository.isAdminUser(request.verifiedBy());
        if (!isAdmin) {
            throw new SecurityException(
                    "User " + request.verifiedBy() + " is not an admin.");
        }

        // 6. Set verified = true
        review.setVerified(true);

        // 7. Update JSONB metadata with verifiedAt and verifiedBy
        java.util.Map<String, Object> metadata = review.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("verifiedAt", Instant.now().toString());
        metadata.put("verifiedBy", request.verifiedBy());
        review.setMetadata(metadata);

        // 8. Save the review
        ProductReview savedReview = reviewRepository.save(review);

        mongoEventLogger.onEvent("REVIEW_VERIFIED", reviewPayload(
                productId,
                savedReview.getId(),
                savedReview.getUserId(),
                Map.of("verifiedBy", request.verifiedBy())
        ));

        // 9. Reload the product with all its reviews and return
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));
    }
}