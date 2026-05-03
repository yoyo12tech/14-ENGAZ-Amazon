package com.team14.amazon.order.neo4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spring Data Neo4j access for the recommendation graph.
 *
 * S3-F12 reads BOUGHT_TOGETHER co-purchase edges using Cypher's undirected
 * match operator (§7.3.2 — edges are stored canonically lower→higher
 * productId but are conceptually undirected, so the query layer is direction-
 * agnostic).
 *
 * Neo4j is a SOFT dependency (CLAUDE.md / §architecture rules) — query
 * failures degrade to an empty result list rather than propagating.
 */
@Repository
public class ProductNodeRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductNodeRepository.class);

    private final Neo4jClient neo4jClient;

    public ProductNodeRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * Find products co-purchased with the seed productId, ranked by
     * coPurchaseCount descending. Each row: { productId: Long, score: Number }.
     *
     * The undirected operator `-[r:BOUGHT_TOGETHER]-` matches canonical-
     * direction edges in either traversal direction (§7.3.2).
     */
    public List<Map<String, Object>> findCoPurchases(Long productId) {
        try {
            Collection<Map<String, Object>> rows = neo4jClient.query("""
                    MATCH (p:Product {productId: $productId})-[r:BOUGHT_TOGETHER]-(other:Product)
                    RETURN other.productId AS productId, r.coPurchaseCount AS score
                    ORDER BY score DESC
                    """)
                    .bind(productId).to("productId")
                    .fetch()
                    .all();
            return new ArrayList<>(rows);
        } catch (Exception e) {
            log.warn("[Neo4j] findCoPurchases failed for productId={} (soft dependency): {}",
                    productId, e.getMessage());
            return List.of();
        }
    }
}
