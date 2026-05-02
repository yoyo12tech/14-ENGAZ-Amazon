package com.team14.amazon.product.adapter;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;

/**
 * M2 §3.8 Adapter Pattern:
 * Converts Elasticsearch Hit → ProductSearchDocument safely.
 */
public class ElasticsearchHitAdapter {

    public ProductSearchDocument adapt(Hit<ProductSearchDocument> hit) {

        if (hit == null) {
            return null;
        }

        ProductSearchDocument doc = hit.source();

        if (doc == null) {
            return null;
        }

        // Ensure ID is always set (important for DTO mapping)
        if (doc.getId() == null) {
            doc.setId(hit.id());
        }

        return doc;
    }
}