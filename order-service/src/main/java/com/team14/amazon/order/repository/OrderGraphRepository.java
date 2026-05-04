package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.OrderItem;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class OrderGraphRepository {

    private final Neo4jClient neo4jClient;

    public OrderGraphRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    // ─── Idempotency Check ───────────────────────────────────────────────
    public boolean isOrderRecorded(Long orderId) {
        return neo4jClient.query("""
                MATCH (r:RecordedOrder {orderId: $orderId})
                RETURN r
                """)
                .bind(orderId).to("orderId")
                .fetch().first().isPresent();
    }

    // ─── Ensure Product Node ─────────────────────────────────────────────
    public void ensureProductNode(Map<String, Object> product) {
        neo4jClient.query("""
                MERGE (p:Product {productId: $id})
                SET p.name = $name,
                    p.category = $category
                """)
                .bind(product.get("id")).to("id")
                .bind(product.get("name")).to("name")
                .bind(product.get("category")).to("category")
                .run();
    }

    // ─── Record BOUGHT_TOGETHER Pairs ────────────────────────────────────
    public void recordCoPurchasePairs(Long orderId, List<OrderItem> items) {
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()                      // ← sort so p1 < p2 is always true
                .toList();

        for (int i = 0; i < productIds.size(); i++) {
            for (int j = i + 1; j < productIds.size(); j++) {
                neo4jClient.query("""
                     MATCH (a:Product {productId: $p1}),
                           (b:Product {productId: $p2})
                     MERGE (a)-[r:BOUGHT_TOGETHER]->(b)
                     ON CREATE SET r.coPurchaseCount = 1,
                                   r.lastCoPurchaseDate = datetime(),
                                   r.recordedOrderIds = [$orderId]
                     ON MATCH SET r.coPurchaseCount = r.coPurchaseCount + 1,
                                  r.lastCoPurchaseDate = datetime(),
                                  r.recordedOrderIds = r.recordedOrderIds + $orderId
                    """)
                        .bind(productIds.get(i)).to("p1")
                        .bind(productIds.get(j)).to("p2")
                        .bind(orderId).to("orderId")
                        .run();
            }
        }
        // sentinel last
        neo4jClient.query("CREATE (:RecordedOrder {orderId: $orderId})")
                .bind(orderId).to("orderId")
                .run();
    }
}
