package com.team14.amazon.product.controller;

import com.team14.amazon.product.dto.ProductSearchResultDTO;
import com.team14.amazon.product.service.ProductFullTextSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductSearchController {

    private final ProductFullTextSearchService searchService;

    public ProductSearchController(ProductFullTextSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/products/search/full-text
     * M2 §10.2.1 — S2-F10 Full-Text Product Search
     * Distinct from M1's GET /api/products/search (category+price, PostgreSQL)
     */
    @GetMapping("/search/full-text")
    public ResponseEntity<List<ProductSearchResultDTO>> fullTextSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {

        if ((minRating != null && (minRating < 0 || minRating > 5)) ||
                (maxRating != null && (maxRating < 0 || maxRating > 5)) ||
                (minRating != null && maxRating != null && minRating > maxRating)) {
            return ResponseEntity.badRequest().build();
        }

        List<ProductSearchResultDTO> results = searchService.fullTextSearch(
                query, category, brand, status, minPrice, maxPrice, minRating, maxRating);
        return ResponseEntity.ok(results);
    }
}