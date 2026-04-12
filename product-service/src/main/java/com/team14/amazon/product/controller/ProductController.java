package com.team14.amazon.product.controller;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductReview;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.service.ProductReviewService;

import com.team14.amazon.product.dto.LowStockAlertDTO;
import com.team14.amazon.product.dto.ProductSalesDTO;
import com.team14.amazon.product.dto.TopProductDTO;
import com.team14.amazon.product.dto.VerifyReviewRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;


import com.team14.amazon.product.service.ProductService;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductReviewService reviewService;

    public ProductController(ProductService productService, ProductReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // Keep explicit literal paths in the top section to avoid path-variable ambiguity.
    @GetMapping("/stock/low-stock")
    public ResponseEntity<List<LowStockAlertDTO>> getLowStockAlerts(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.getLowStockAlerts(threshold));
    }

    @GetMapping("/specifications/search")
    public ResponseEntity<List<Product>> searchBySpecification(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.searchBySpecification(key, value, status));
    }

    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopProductDTO>> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getTopRatedProducts(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Product createdProduct = productService.createProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        return productService.updateProduct(id, updatedProduct)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/discontinue")
    public ResponseEntity<?> discontinueProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.discontinueProduct(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    // S2-F3: Get product sales summary
    @GetMapping("/{id}/sales")
    public ResponseEntity<?> getProductSalesSummary(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end   = LocalDate.parse(endDate);
            ProductSalesDTO dto = productService.getProductSalesSummary(id, start, end);
            return ResponseEntity.ok(dto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid date format. Use yyyy-MM-dd.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<List<Product>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        // Return 200 [] for invalid Enum parameters gracefully
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }

    // S2-F1: Search products by category and price range
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) com.team14.amazon.product.model.ProductCategory category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        try {
            List<Product> results = productService.searchProducts(category, minPrice, maxPrice);
            return ResponseEntity.ok(results != null ? results : java.util.Collections.emptyList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // S2-F2: Update product specifications (JSONB partial update)
    @PutMapping("/{id}/specifications")
    public ResponseEntity<?> updateSpecifications(
            @PathVariable Long id,
            @RequestBody Map<String, Object> specifications) {
        return productService.updateSpecifications(id, specifications)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── S2-F7: POST /api/products/{id}/reviews ─────────────────────────────

    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long id,
            @RequestBody ProductReview review) {
        try {
            ProductReview savedReview = reviewService.addReviewWithRatingRecalculation(id, review);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{productId}/reviews/{reviewId}")
    public ResponseEntity<ProductReview> getReviewByProductAndId(
            @PathVariable Long productId,
            @PathVariable Long reviewId) {
        return reviewService.getReviewByProductAndId(productId, reviewId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{productId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReviewByProductAndId(
            @PathVariable Long productId,
            @PathVariable Long reviewId) {
        return reviewService.getReviewByProductAndId(productId, reviewId)
                .map(ignoredReview -> {
                    reviewService.deleteReview(reviewId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // S2-F8: PUT /api/products/{productId}/reviews/{reviewId}/verify
    @PutMapping("/{productId}/reviews/{reviewId}/verify")
    public ResponseEntity<?> verifyReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestBody VerifyReviewRequest request) {
        try {
            Product updatedProduct = reviewService.verifyReview(productId, reviewId, request);
            return ResponseEntity.ok(updatedProduct);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


}