package com.team14.amazon.product.repository.elasticsearch;

import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductSearchDocument, String> {
}