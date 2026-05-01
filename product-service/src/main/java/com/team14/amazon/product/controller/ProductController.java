package com.team14.amazon.product.controller;

import com.team14.amazon.product.dto.LowStockAlertDTO;
import com.team14.amazon.product.dto.ProductSalesDTO;
import com.team14.amazon.product.dto.TopProductDTO;
import com.team14.amazon.product.dto.VerifyReviewRequest;
import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductReview;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.service.ProductReviewService;
import com.team14.amazon.product.service.ProductService;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductReviewService reviewService;

    public ProductController(ProductService productService,
                             ProductReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    // CRUD first
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
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
            Product created = productService.createProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // PUT: invalidate detail + all feature caches
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id,
                                                 @RequestBody Product updatedProduct) {
        return productService.updateProduct(id, updatedProduct)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Features in chronological order (S2-F1 -> S2-F9)
    // M2 4.4.1 - S2-F1 cached, TTL 5 min
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) com.team14.amazon.product.model.ProductCategory category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        try {
            List<Product> results = productService.searchProducts(category, minPrice, maxPrice);
            return ResponseEntity.ok(results != null ? results : Collections.emptyList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // S2-F2 WRITE: invalidate
    @PutMapping("/{id}/specifications")
    public ResponseEntity<?> updateSpecifications(
            @PathVariable Long id,
            @RequestBody Map<String, Object> specifications) {
        return productService.updateSpecifications(id, specifications)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // M2 4.4.1 - S2-F3 cached, TTL 10 min
    @GetMapping("/{id}/sales")
    public ResponseEntity<?> getProductSalesSummary(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
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

    // S2-F4 WRITE: invalidate
    @PutMapping("/{id}/discontinue")
    public ResponseEntity<?> discontinueProduct(@PathVariable Long id) {
        try {
            Product p = productService.discontinueProduct(id);
            return ResponseEntity.ok(p);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // M2 4.4.1 - S2-F5 cached, TTL 5 min
    @GetMapping("/specifications/search")
    public ResponseEntity<List<Product>> searchBySpecification(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.searchBySpecification(key, value, status));
    }

    // M2 4.4.1 - S2-F6 cached, TTL 10 min
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopProductDTO>> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getTopRatedProducts(limit));
    }

    // S2-F7 WRITE: rating changed - invalidate product + review caches
    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable Long id,
            @RequestBody ProductReview review) {
        try {
            ProductReview saved = reviewService.addReviewWithRatingRecalculation(id, review);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // M2 4.4.2 product-review GET-by-ID cached, TTL 15 min
    @GetMapping("/{productId}/reviews/{reviewId}")
    public ResponseEntity<ProductReview> getReviewByProductAndId(
            @PathVariable Long productId,
            @PathVariable Long reviewId) {
        return reviewService.getReviewByProductAndId(productId, reviewId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // S2-F8 WRITE (M2 4.4.4 - explicitly a write): invalidate
    @PutMapping("/{productId}/reviews/{reviewId}/verify")
    public ResponseEntity<?> verifyReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestBody VerifyReviewRequest request) {
        try {
            Product updated = reviewService.verifyReview(productId, reviewId, request);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // DELETE review WRITE: invalidate
    @DeleteMapping("/{productId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReviewByProductAndId(
            @PathVariable Long productId,
            @PathVariable Long reviewId) {
        return reviewService.getReviewByProductAndId(productId, reviewId)
                .map(ignored -> {
                    reviewService.deleteReview(reviewId, productId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // M2 4.4.1 - S2-F9 cached, TTL 10 min
    @GetMapping("/stock/low-stock")
    public ResponseEntity<List<LowStockAlertDTO>> getLowStockAlerts(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.getLowStockAlerts(threshold));
    }


    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<List<Product>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ignored) {
        return ResponseEntity.ok(Collections.emptyList());
    }
}