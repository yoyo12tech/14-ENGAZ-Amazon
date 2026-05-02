package com.team14.amazon.product.service;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;
import com.team14.amazon.product.observer.EntityObserver;
import com.team14.amazon.product.repository.ProductRepository;
import com.team14.amazon.product.repository.elasticsearch.ProductSearchRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ProductIndexService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ElasticsearchIndexService esIndexService;
    private final EntityObserver mongoEventLogger;
    private final CacheInvalidationService cacheInvalidationService;

    private static final List<String> INDEXED_FIELDS = List.of(
            "id", "name", "description", "category", "brand",
            "price", "stockQuantity", "rating", "status"
    );

    public ProductIndexService(ProductRepository productRepository,
                               ProductSearchRepository searchRepository,
                               ElasticsearchIndexService esIndexService,
                               @Qualifier("mongoEventLogger") EntityObserver mongoEventLogger,
                               CacheInvalidationService cacheInvalidationService) {
        this.productRepository = productRepository;
        this.searchRepository = searchRepository;
        this.esIndexService = esIndexService;
        this.mongoEventLogger = mongoEventLogger;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    /**
     * Explicit indexing — called by POST /api/products/{id}/index
     * source = "explicit" per M2 §10.2.2 step d
     */
    public ProductSearchDocument indexProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        // Index in Elasticsearch
        esIndexService.indexProduct(product);

        // Retrieve stored document
        ProductSearchDocument stored = searchRepository.findById(String.valueOf(productId))
                .orElseThrow(() -> new RuntimeException("ES indexing failed for product: " + productId));

        // Observer event
        Map<String, Object> details = new HashMap<>();
        details.put("indexedFields", INDEXED_FIELDS);
        details.put("source", "explicit");
        details.put("productId", productId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("details", details);

        mongoEventLogger.onEvent("INDEXED", payload);

        // invalidate search cache
        cacheInvalidationService.evictByPattern("product-service::S2-F10::*");

        return stored;
    }
}
