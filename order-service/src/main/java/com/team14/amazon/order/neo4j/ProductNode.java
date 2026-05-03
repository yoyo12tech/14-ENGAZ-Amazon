package com.team14.amazon.order.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Neo4j node representing a product in the recommendation graph (§7.3.1).
 *
 * Read by S3-F12; written by S3-F11 (teammate, separate branch).
 *
 * Per §7.3 staleness note: name/category are advisory snapshots — the
 * authoritative product data is always re-read from PostgreSQL on every
 * recommendation read (S3-F12 step d), so user-visible DTOs are never stale
 * even if the node itself is.
 */
@Node("Product")
public class ProductNode {

    @Id
    @Property("productId")
    private Long productId;

    private String name;
    private String category;

    public ProductNode() {}

    public Long   getProductId()         { return productId; }
    public void   setProductId(Long v)   { this.productId = v; }

    public String getName()              { return name; }
    public void   setName(String v)      { this.name = v; }

    public String getCategory()          { return category; }
    public void   setCategory(String v)  { this.category = v; }
}
