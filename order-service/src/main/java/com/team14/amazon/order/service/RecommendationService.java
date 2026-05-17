package com.team14.amazon.order.service;

import com.team14.amazon.contracts.dto.ProductDTO;
import com.team14.amazon.contracts.feign.ProductServiceClient;
import com.team14.amazon.order.adapter.Neo4jRecordAdapter;
import com.team14.amazon.order.dto.ProductRecommendationDTO;
import com.team14.amazon.order.repository.ProductNodeRepository;
import com.team14.amazon.order.repository.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.order.repository.OrderGraphRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3-F12 — "Customers Also Bought" recommendations (§10.3.3).
 *
 * Orchestrates:
 * b) seed-product 404 check in PostgreSQL
 * c) co-purchase graph traversal in Neo4j (undirected BOUGHT_TOGETHER)
 * d) enrichment + ACTIVE/exists filter against PostgreSQL
 * e) top-N truncation (default limit = 5)
 * f) Redis caching (5-min TTL via RedisConfig — §8.1 search format)
 *
 * Cache key: `order-service::S3-F12::&lt;productId&gt;::&lt;limit&gt;`
 * (Spring's
 *
 * @Cacheable concatenates value + key separator + key; the value already
 *            carries the service::feature prefix per RedisConfig).
 *
 *            Cache invalidation on co-purchase writes is owned by S3-F11
 *            (teammate),
 *            which calls
 *            {@code CacheEvictionService.evictRecommendationCache()} per
 *            the §4.4.4 NoSQL-writer → cached-reader rule.
 *
 *            No Observer/Factory here: this is a pure read endpoint — no events
 *            emitted,
 *            no PG/Mongo/Neo4j writes. ANALYTICS_VIEWED-style logging is not
 *            required
 *            by §10.3.3.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final int DEFAULT_LIMIT = 5;

    private final OrderGraphRepository orderGraphRepository;
    private final ProductServiceClient productServiceClient;
    private final Neo4jRecordAdapter neo4jRecordAdapter;

    public RecommendationService(OrderGraphRepository orderGraphRepository,
                                 ProductServiceClient productServiceClient,
                                 Neo4jRecordAdapter neo4jRecordAdapter) {
        this.orderGraphRepository = orderGraphRepository;
        this.productServiceClient = productServiceClient;
        this.neo4jRecordAdapter = neo4jRecordAdapter;
    }

    @Cacheable(value = "order-service::S3-F12", key = "#productId + '::' + #limit")
    public List<ProductRecommendationDTO> getRecommendations(Long productId, int limit) {

        // (b) Seed product must exist in PG → 404 otherwise. — §5 S3-F12 change 1
        try {
            productServiceClient.getProduct(productId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
        } catch (FeignException e) {
            log.error("product-service unavailable for seed check: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable");
        }

        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;

        // (c) Neo4j traversal — soft dep: empty list on any failure (logged in repo).
        List<Map<String, Object>> rows = orderGraphRepository.findCoPurchases(productId);

        List<Long> candidateIds = rows.stream()
                .map(row -> ((Number) row.get("productId")).longValue())
                .filter(id -> !id.equals(productId))
                .collect(Collectors.toList());

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // (d, e) Enrich from PG via Feign batch — §5 S3-F12 change 2
        Map<Long, ProductDTO> productMap;
        try {
            List<ProductDTO> products = productServiceClient.getProductsBatch(candidateIds);
            productMap = products.stream()
                    .filter(p -> "ACTIVE".equalsIgnoreCase(p.status()))
                    .collect(Collectors.toMap(ProductDTO::id, p -> p));
        } catch (FeignException e) {
            log.error("product-service unavailable for recommendation enrichment: {}", e.getMessage());
            return List.of(); // soft dependency per §2.4 [cite: 31]
        }

        List<ProductRecommendationDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long candidateId = ((Number) row.get("productId")).longValue();
            ProductDTO p = productMap.get(candidateId);
            if (p == null) continue;

            ProductRecommendationDTO dto = neo4jRecordAdapter.adapt(row);
            dto.setName(p.name());
            dto.setCategory(p.category());
            dto.setBrand(p.brand());
            dto.setPrice(p.price());
            result.add(dto);

            if (result.size() >= effectiveLimit)
                break;
        }

        log.debug("[S3-F12] productId={} limit={} returned {} recommendations",
                productId, effectiveLimit, result.size());
        return result;
    }

    // ── small coercion helpers (PG returns native types via JDBC native SQL) ───
    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static Double asDouble(Object v) {
        if (v == null)
            return null;
        if (v instanceof Number n)
            return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}