package com.team14.amazon.product.controller;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductReview;
import com.team14.amazon.product.service.ProductReviewService;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductReviewController {

    private final ProductReviewService reviewService;

    public ProductReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ── Flat /api/reviews CRUD ──────────────────────────────────────────────

    @GetMapping("/reviews")
    public ResponseEntity<List<ProductReview>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<ProductReview> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(@RequestBody ProductReview review) {
        try {
            ProductReview createdReview = reviewService.createReview(review);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/reviews/{id}")
    public ResponseEntity<ProductReview> updateReview(@PathVariable Long id,
                                                      @RequestBody ProductReview updatedReview) {
        return reviewService.updateReview(id, updatedReview)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        if (reviewService.deleteReview(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/reviews/product/{productId}")
    public ResponseEntity<List<ProductReview>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    // ── S2-F7: POST /api/products/{id}/reviews ─────────────────────────────

    @PostMapping("/products/{id}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long id,
            @RequestBody ProductReview review) {
        try {
            Product updatedProduct = reviewService.addReviewWithRatingRecalculation(id, review);
            return ResponseEntity.ok(updatedProduct);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}