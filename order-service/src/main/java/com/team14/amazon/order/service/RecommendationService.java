package com.team14.amazon.order.service;

import com.team14.amazon.order.adapter.Neo4jRecordAdapter;
import com.team14.amazon.order.dto.ProductRecommendationDTO;
import com.team14.amazon.order.repository.ProductNodeRepository;
import com.team14.amazon.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * S3-F12 — "Customers Also Bought" recommendations (§10.3.3).
 *
 * Orchestrates:
 *   b) seed-product 404 check in PostgreSQL
 *   c) co-purchase graph traversal in Neo4j (undirected BOUGHT_TOGETHER)
 *   d) enrichment + ACTIVE/exists filter against PostgreSQL
 *   e) top-N truncation (default limit = 5)
 *   f) Redis caching (5-min TTL via RedisConfig — §8.1 search format)
 *
 * Cache key: `order-service::S3-F12::&lt;productId&gt;::&lt;limit&gt;` (Spring's
 * @Cacheable concatenates value + key separator + key; the value already
 * carries the service::feature prefix per RedisConfig).
 *
 * Cache invalidation on co-purchase writes is owned by S3-F11 (teammate),
 * which calls {@code CacheEvictionService.evictRecommendationCache()} per
 * the §4.4.4 NoSQL-writer → cached-reader rule.
 *
 * No Observer/Factory here: this is a pure read endpoint — no events emitted,
 * no PG/Mongo/Neo4j writes. ANALYTICS_VIEWED-style logging is not required
 * by §10.3.3.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final int    DEFAULT_LIMIT = 5;

    private final ProductNodeRepository productNodeRepository;
    private final OrderRepository       orderRepository;
    private final Neo4jRecordAdapter    neo4jRecordAdapter;

    public RecommendationService(ProductNodeRepository productNodeRepository,
                                 OrderRepository orderRepository,
                                 Neo4jRecordAdapter neo4jRecordAdapter) {
        this.productNodeRepository = productNodeRepository;
        this.orderRepository       = orderRepository;
        this.neo4jRecordAdapter    = neo4jRecordAdapter;
    }

    @Cacheable(
            value = "order-service::S3-F12",
            key   = "#productId + '::' + #limit"
    )
    public List<ProductRecommendationDTO> getRecommendations(Long productId, int limit) {

        // (b) Seed product must exist in PG → 404 otherwise.
        //     Reuses the existing native-SQL cross-service helper on OrderRepository.
        Map<String, Object> seedRow = orderRepository.findProductById(productId);
        if (seedRow == null || seedRow.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + productId);
        }

        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;

        // (c) Neo4j traversal — soft dep: empty list on any failure (logged in repo).
        List<Map<String, Object>> rows = productNodeRepository.findCoPurchases(productId);

        // (d, e) Enrich from PG; drop seed itself, missing rows, and non-ACTIVE
        //         products. Truncate as soon as we have `effectiveLimit` survivors.
        List<ProductRecommendationDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ProductRecommendationDTO dto = neo4jRecordAdapter.adapt(row);

            if (dto.getProductId() == null || dto.getProductId().equals(productId)) {
                continue; // exclude seed (defensive — Cypher already excludes)
            }

            Map<String, Object> pgRow = orderRepository.findProductById(dto.getProductId());
            if (pgRow == null || pgRow.isEmpty()) {
                continue; // product deleted in PG
            }
            String status = pgRow.get("status") == null ? null : pgRow.get("status").toString();
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                continue; // INACTIVE / OUT_OF_STOCK must not appear (§10.3.3 d)
            }

            dto.setName    (asString(pgRow.get("name")));
            dto.setCategory(asString(pgRow.get("category")));
            dto.setBrand   (asString(pgRow.get("brand")));
            dto.setPrice   (asDouble(pgRow.get("price")));
            result.add(dto);

            if (result.size() >= effectiveLimit) break;
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
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
