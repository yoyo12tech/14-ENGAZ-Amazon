package com.team14.amazon.product.controller;

import com.team14.amazon.product.model.ProductReview;
import com.team14.amazon.product.service.CacheInvalidationService;
import com.team14.amazon.product.service.ProductReviewService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ProductReviewController {

    private final ProductReviewService reviewService;
    private final CacheInvalidationService cacheInvalidationService;

    public ProductReviewController(ProductReviewService reviewService,
                                   CacheInvalidationService cacheInvalidationService) {
        this.reviewService = reviewService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    // CRUD first
    @GetMapping
    public ResponseEntity<List<ProductReview>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    // M2 4.4.2 - GET-by-ID cached, TTL 15 min
    @GetMapping("/{id}")
    @Cacheable(value = "product-service::product-review", key = "#id")
    public ResponseEntity<ProductReview> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // WRITE: new review -> invalidate product feature caches
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ProductReview review) {
        try {
            ProductReview created = reviewService.createReview(review);
            Long productId = created.getProduct() != null ? created.getProduct().getId() : null;
            cacheInvalidationService.invalidateReviewCaches(null, productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // WRITE: rating change -> invalidate review detail + product caches
    @PutMapping("/{id}")
    public ResponseEntity<ProductReview> updateReview(@PathVariable Long id,
                                                      @RequestBody ProductReview updatedReview) {
        return reviewService.updateReview(id, updatedReview)
                .map(r -> {
                    Long productId = r.getProduct() != null ? r.getProduct().getId() : null;
                    cacheInvalidationService.invalidateReviewCaches(id, productId);
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // WRITE: invalidate review detail + product caches
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(r -> {
                    Long productId = r.getProduct() != null ? r.getProduct().getId() : null;
                    reviewService.deleteReview(id);
                    cacheInvalidationService.invalidateReviewCaches(id, productId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Feature endpoint after CRUD
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductReview>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }


}