package com.team14.amazon.product.controller;


import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductReview;

import com.team14.amazon.product.service.ProductService;
import com.team14.amazon.product.service.ProductReviewService;




import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.Optional;

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




    // S2-F2: Update product specifications (JSONB partial update)
    @PutMapping("/{id}/specifications")
    public ResponseEntity<?> updateSpecifications(
            @PathVariable Long id,
            @RequestBody Map<String, Object> specifications) {
        return productService.updateSpecifications(id, specifications)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }





}