package com.team14.amazon.order.adapter;

import com.team14.amazon.order.dto.ProductRecommendationDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DP-8 Adapter Pattern (§3.8) — converts a raw Neo4j result row
 * (Map&lt;String, Object&gt; from {@link org.springframework.data.neo4j.core.Neo4jClient}'s
 * fetch.all()) into the service-layer {@link ProductRecommendationDTO}.
 *
 * Only productId and score are populated here; the remaining fields
 * (name, category, brand, price) are enriched from PostgreSQL by the
 * service layer per §10.3.3 step d (PG is the authoritative source).
 */
@Component
public class Neo4jRecordAdapter {

    public ProductRecommendationDTO adapt(Map<String, Object> record) {
        Object idVal    = record.get("productId");
        Object scoreVal = record.get("score");

        Long productId = idVal    == null ? null : ((Number) idVal).longValue();
        int  score     = scoreVal == null ? 0    : ((Number) scoreVal).intValue();

        return ProductRecommendationDTO.builder()
                .productId(productId)
                .score(score)
                .build();
    }
}
