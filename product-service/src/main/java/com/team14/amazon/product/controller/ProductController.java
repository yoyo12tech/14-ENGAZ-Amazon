package com.team14.amazon.product.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team14.amazon.product.dto.TopProductDTO;
import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.service.ProductReviewService;
import com.team14.amazon.product.service.ProductService;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    // S2-F1: Search products by category and price range
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        try {
            List<Product> results = productService.searchProducts(category, minPrice, maxPrice);
            return ResponseEntity.ok(results);
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


    @PutMapping("/{id}/discontinue")
    public ResponseEntity<Product> discontinueProduct(@PathVariable Long id) {
        Product product = productService.discontinueProduct(id);
        return ResponseEntity.ok(product); // 200 with the updated product body
    }

    @GetMapping("/specifications/search")
    public ResponseEntity<List<Product>> searchBySpecification(
        @RequestParam String key,
        @RequestParam String value,
        @RequestParam(required = false) String status) {
        return ResponseEntity.ok(productService.filterBySpecification(key, value, status));
}


// S2-F6: Top Rated Products Report
    @GetMapping("/reports/top-rated")
    public ResponseEntity<List<TopProductDTO>> getTopRatedProducts(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        
        List<TopProductDTO> report = productService.getTopRatedProductsReport(limit);
        return ResponseEntity.ok(report);
    }

}