package com.team14.amazon.product.service;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;
import com.team14.amazon.product.repository.elasticsearch.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service-level hook for Elasticsearch indexing.
 * Called from ProductService on every write — NOT inlined in controllers.
 * M2 §4.5 "Implement via ... a service-level hook; do not inline the ES call
 * into every CRUD controller method."
 */
@Service
public class ElasticsearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexService.class);
    private final ProductSearchRepository productSearchRepository;

    public ElasticsearchIndexService(ProductSearchRepository productSearchRepository) {
        this.productSearchRepository = productSearchRepository;
    }

    /**
     * Indexes (creates or updates) the product document in Elasticsearch.
     * Called on POST /api/products and PUT /api/products/{id}.
     */
    public void indexProduct(Product product) {
        try {
            ProductSearchDocument doc = toDocument(product);
            productSearchRepository.save(doc);
            log.debug("Indexed product {} in Elasticsearch", product.getId());
        } catch (Exception e) {
            // Soft dependency — never crash the PG transaction on ES failure
            log.warn("Failed to index product {} in Elasticsearch: {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Removes the product document from Elasticsearch.
     * Called on DELETE /api/products/{id}.
     */
    public void deleteFromIndex(Long productId) {
        try {
            productSearchRepository.deleteById(String.valueOf(productId));
            log.debug("Removed product {} from Elasticsearch index", productId);
        } catch (Exception e) {
            log.warn("Failed to remove product {} from Elasticsearch: {}", productId, e.getMessage());
        }
    }

    private ProductSearchDocument toDocument(Product product) {
        ProductSearchDocument doc = new ProductSearchDocument();
        doc.setId(String.valueOf(product.getId()));
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setCategory(product.getCategory() != null ? product.getCategory().name() : null);
        doc.setBrand(product.getBrand());
        doc.setPrice(product.getPrice());
        doc.setStockQuantity(product.getStockQuantity());
        doc.setRating(product.getRating());
        doc.setStatus(product.getStatus() != null ? product.getStatus().name() : null);
        return doc;
    }
}