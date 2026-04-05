package com.team14.amazon.product.service;




import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.repository.ProductRepository;
import com.team14.amazon.product.repository.ProductReviewRepository;
import com.team14.amazon.product.model.ProductReview;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ProductReviewService(ProductReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public List<ProductReview> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Optional<ProductReview> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    // --- Create a review ---
    public ProductReview createReview(ProductReview review) {
        Product product = productRepository.findById(review.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        review.setProduct(product);

        // Update product rating
        int totalRatings = product.getTotalRatings() + 1;
        double totalScore = product.getRating() * product.getTotalRatings() + review.getRating();
        double newRating = totalScore / totalRatings;

        product.setRating(newRating);
        product.setTotalRatings(totalRatings);

        productRepository.save(product);

        return reviewRepository.save(review);
    }

    // --- Update review ---
    public Optional<ProductReview> updateReview(Long id, ProductReview updatedReview) {
        return reviewRepository.findById(id).map(existingReview -> {
            Product product = existingReview.getProduct();

            // Back out old rating, substitute new rating
            double totalScore = product.getRating() * product.getTotalRatings()
                    - existingReview.getRating()
                    + updatedReview.getRating();
            double newRating = totalScore / product.getTotalRatings();

            product.setRating(newRating);
            productRepository.save(product);

            existingReview.setRating(updatedReview.getRating());
            existingReview.setTitle(updatedReview.getTitle());
            existingReview.setComment(updatedReview.getComment());
            existingReview.setVerified(updatedReview.getVerified());
            existingReview.setMetadata(updatedReview.getMetadata());
            return reviewRepository.save(existingReview);
        });
    }

    // --- Delete review ---
    public boolean deleteReview(Long id) {
        return reviewRepository.findById(id).map(review -> {
            Product product = review.getProduct();
            int totalRatings = product.getTotalRatings() - 1;

            if (totalRatings == 0) {
                product.setRating(0.0);
            } else {
                double totalScore = product.getRating() * product.getTotalRatings() - review.getRating();
                product.setRating(totalScore / totalRatings);
            }

            product.setTotalRatings(totalRatings);
            productRepository.save(product);

            reviewRepository.delete(review);
            return true;
        }).orElse(false);
    }

    // --- Get reviews by product ---
    public List<ProductReview> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}