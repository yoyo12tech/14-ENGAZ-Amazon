package com.team14.amazon.order.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j node representing a product in the BOUGHT_TOGETHER recommendation graph.
 *
 * name and category are advisory snapshots from PG at co-purchase time.
 * Authoritative product data is always re-read from PostgreSQL for API responses.
 *
 * Canonical BOUGHT_TOGETHER direction: lower productId → higher productId.
 * S3-F12 queries use undirected Cypher match, so direction is transparent.
 */
@Node("Product")
public class ProductNode {

    @Id
    @GeneratedValue
    private Long id;

    /** Corresponds to Product.id in PostgreSQL */
    private Long productId;

    /** Advisory snapshot — not used for API responses */
    private String name;

    /** For optional category-based filtering in recommendations */
    private String category;

    @Relationship(type = "BOUGHT_TOGETHER", direction = Relationship.Direction.OUTGOING)
    private List<BoughtTogetherRelationship> boughtTogether = new ArrayList<>();

    public ProductNode() {}

    public ProductNode(Long productId, String name, String category) {
        this.productId = productId;
        this.name      = name;
        this.category  = category;
    }

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Long getProductId()                       { return productId; }
    public void setProductId(Long productId)         { this.productId = productId; }

    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }

    public String getCategory()                      { return category; }
    public void setCategory(String category)         { this.category = category; }

    public List<BoughtTogetherRelationship> getBoughtTogether()                               { return boughtTogether; }
    public void setBoughtTogether(List<BoughtTogetherRelationship> boughtTogether)            { this.boughtTogether = boughtTogether; }
}