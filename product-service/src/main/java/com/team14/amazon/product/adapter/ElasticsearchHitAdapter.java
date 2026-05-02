package com.team14.amazon.product.adapter;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;

/**
 * Adapter pattern (M2 §3.8): converts an Elasticsearch Hit<ProductSearchDocument>
 * into a ProductSearchDocument for service-layer consumption.
 */
public class ElasticsearchHitAdapter {

    public ProductSearchDocument adapt(Hit<ProductSearchDocument> hit) {
        ProductSearchDocument doc = hit.source();
        if (doc != null && doc.getId() == null) {
            doc.setId(hit.id());
        }
        return doc;
    }
}