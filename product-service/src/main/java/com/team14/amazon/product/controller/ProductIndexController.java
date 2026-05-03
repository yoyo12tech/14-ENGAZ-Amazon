package com.team14.amazon.product.controller;

import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;
import com.team14.amazon.product.service.ProductIndexService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/products")
public class ProductIndexController {

    private final ProductIndexService productIndexService;

    public ProductIndexController(ProductIndexService productIndexService) {
        this.productIndexService = productIndexService;
    }

    /**
     * POST /api/products/{id}/index
     * M2 §10.2.2 — S2-F11 Index Product for Search
     */
    @PostMapping("/{id}/index")
    public ResponseEntity<?> indexProduct(@PathVariable Long id) {
        try {
            ProductSearchDocument doc = productIndexService.indexProduct(id);
            return ResponseEntity.ok(doc);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
