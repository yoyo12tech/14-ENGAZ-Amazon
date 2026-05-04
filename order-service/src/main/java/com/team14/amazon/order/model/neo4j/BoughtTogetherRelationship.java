package com.team14.amazon.order.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BOUGHT_TOGETHER relationship between two ProductNodes.
 *
 * Canonical direction: lower productId → higher productId (always).
 * S3-F12 recommendations use undirected Cypher match operator.
 *
 * recordedOrderIds: idempotency marker per spec Section 10.3.2 step (e).
 * coPurchaseCount is incremented atomically with appending orderId to
 * recordedOrderIds in a single Cypher transaction (step g).
 */
@RelationshipProperties
public class BoughtTogetherRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private ProductNode target;

    /** Incremented each time the two products appear in a new delivered order. */
    private Integer coPurchaseCount = 0;

    /** Updated on each increment. */
    private LocalDateTime lastCoPurchaseDate;

    /**
     * Idempotency marker: accumulates orderIds that have already contributed
     * to this edge's coPurchaseCount. S3-F11 checks this before mutating.
     */
    private List<Long> recordedOrderIds = new ArrayList<>();

    public BoughtTogetherRelationship() {}

    public BoughtTogetherRelationship(ProductNode target, Long firstOrderId) {
        this.target              = target;
        this.coPurchaseCount     = 1;
        this.lastCoPurchaseDate  = LocalDateTime.now();
        this.recordedOrderIds    = new ArrayList<>(List.of(firstOrderId));
    }

    public Long getId()                                    { return id; }
    public void setId(Long id)                             { this.id = id; }

    public ProductNode getTarget()                         { return target; }
    public void setTarget(ProductNode target)              { this.target = target; }

    public Integer getCoPurchaseCount()                    { return coPurchaseCount; }
    public void setCoPurchaseCount(Integer c)              { this.coPurchaseCount = c; }

    public LocalDateTime getLastCoPurchaseDate()           { return lastCoPurchaseDate; }
    public void setLastCoPurchaseDate(LocalDateTime d)     { this.lastCoPurchaseDate = d; }

    public List<Long> getRecordedOrderIds()                { return recordedOrderIds; }
    public void setRecordedOrderIds(List<Long> ids)        { this.recordedOrderIds = ids; }
}