package com.team14.amazon.order.service;

import com.team14.amazon.contracts.dto.ProductDTO;
import com.team14.amazon.contracts.feign.ProductServiceClient;
import com.team14.amazon.order.adapter.Neo4jRecordAdapter;
import com.team14.amazon.order.dto.ProductRecommendationDTO;
import com.team14.amazon.order.repository.OrderGraphRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3-F12 — "Customers Also Bought" recommendations (M3 refactor).
 *
 * M2 cross-service SQL removed:
 *   - orderRepository.findProductById(seedProductId)   → ProductServiceClient.productExists()
 *   - orderRepository.findProductById(candidateId)     → ProductServiceClient.getProductsBatch()
 *
 * The Neo4j traversal is unchanged. All enrichment and ACTIVE-filtering
 * now uses Feign data from product-service instead of direct SQL.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final int DEFAULT_LIMIT = 5;

    private final OrderGraphRepository orderGraphRepository;
    private final ProductServiceClient productServiceClient;
    private final Neo4jRecordAdapter   neo4jRecordAdapter;

    public RecommendationService(OrderGraphRepository orderGraphRepository,
                                 ProductServiceClient productServiceClient,
                                 Neo4jRecordAdapter neo4jRecordAdapter) {
        this.orderGraphRepository = orderGraphRepository;
        this.productServiceClient = productServiceClient;
        this.neo4jRecordAdapter   = neo4jRecordAdapter;
    }

    @Cacheable(value = "order-service::S3-F12", key = "#productId + '::' + #limit")
    public List<ProductRecommendationDTO> getRecommendations(Long productId, int limit) {

        // ── (b) Seed product must exist → 404 otherwise ───────────────────────
        log.info("Calling ProductServiceClient.productExists with args=[{}]", productId);
        try {
            Map<String, Boolean> existsResult = productServiceClient.productExists(productId);
            log.info("ProductServiceClient.productExists returned successfully");
            if (Boolean.FALSE.equals(existsResult.get("exists"))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found: " + productId);
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productId);
        } catch (FeignException e) {
            log.warn("Feign call to product-service failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Product service temporarily unavailable");
        }

        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;

        // ── (c) Neo4j traversal (unchanged from M2) ───────────────────────────
        List<Map<String, Object>> rows = orderGraphRepository.findCoPurchases(productId);

        // Collect candidate productIds from Neo4j result, excluding seed
        List<Long> candidateIds = rows.stream()
                .map(row -> neo4jRecordAdapter.adapt(row))
                .map(ProductRecommendationDTO::getProductId)
                .filter(id -> id != null && !id.equals(productId))
                .collect(Collectors.toList());

        if (candidateIds.isEmpty()) {
            log.debug("[S3-F12] productId={} — no co-purchase candidates in Neo4j", productId);
            return new ArrayList<>();
        }

        // ── (d) Batch Feign → product-service for enrichment ──────────────────
        // Fetch up to 2×limit candidates to allow filtering of INACTIVE products
        List<Long> batchIds = candidateIds.stream()
                .limit((long) effectiveLimit * 2)
                .collect(Collectors.toList());

        log.info("Calling ProductServiceClient.getProductsBatch with args=[{}]", batchIds);
        List<ProductDTO> products;
        try {
            products = productServiceClient.getProductsBatch(batchIds);
            log.info("ProductServiceClient.getProductsBatch returned successfully");
        } catch (FeignException e) {
            log.warn("[S3-F12] Feign batch call to product-service failed: {}", e.getMessage());
            return new ArrayList<>(); // soft failure — return empty rather than 500
        }

        // Index by productId for O(1) lookup
        Map<Long, ProductDTO> productMap = products.stream()
                .collect(Collectors.toMap(ProductDTO::id, p -> p));

        // Build score map from Neo4j rows
        Map<Long, Object> scoreMap = rows.stream()
                .map(row -> neo4jRecordAdapter.adapt(row))
                .filter(dto -> dto.getProductId() != null && !dto.getProductId().equals(productId))
                .collect(Collectors.toMap(
                        ProductRecommendationDTO::getProductId,
                        dto -> dto.getScore(),
                        (a, b) -> a));

        // ── (e) Filter INACTIVE, enrich, trim to limit ────────────────────────
        List<ProductRecommendationDTO> result = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            if (result.size() >= effectiveLimit) break;

            ProductDTO product = productMap.get(candidateId);
            if (product == null) continue;                  // not in batch (deleted)
            if (!"ACTIVE".equalsIgnoreCase(product.status())) continue; // INACTIVE/OUT_OF_STOCK

            ProductRecommendationDTO dto = new ProductRecommendationDTO();
            dto.setProductId(product.id());
            dto.setName(product.name());
            dto.setCategory(product.category());
            dto.setBrand(product.brand());
            dto.setPrice(product.price());
            Object score = scoreMap.get(candidateId);
            if (score instanceof Number n) dto.setScore(n.intValue());
            result.add(dto);
        }

        log.debug("[S3-F12] productId={} limit={} returned {} recommendations",
                productId, effectiveLimit, result.size());
        return result;
    }
}