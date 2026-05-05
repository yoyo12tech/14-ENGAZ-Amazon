package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.neo4j.ProductNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ProductNodeRepository extends Neo4jRepository<ProductNode, Long> {

    Optional<ProductNode> findByProductId(Long productId);

    /**
     * S3-F12: find all ProductNodes connected to the seed via BOUGHT_TOGETHER
     * (either direction), ordered by coPurchaseCount descending, limited to top N.
     * Excludes the seed product itself.
     */
    @Query("""
            MATCH (seed:Product {productId: $productId})-[r:BOUGHT_TOGETHER]-(other:Product)
            WHERE other.productId <> $productId
            RETURN other.productId      AS productId,
                   r.coPurchaseCount    AS score
            ORDER BY r.coPurchaseCount  DESC,
                     r.lastCoPurchaseDate DESC
            """)
    List<Map<String, Object>> findCoPurchases(@Param("productId") Long productId);

    java.util.List<ProductNode> findRecommendationsByProductId(
            @Param("productId") Long productId,
            @Param("limit") int limit);
}