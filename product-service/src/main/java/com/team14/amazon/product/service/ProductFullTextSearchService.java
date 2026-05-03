package com.team14.amazon.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.team14.amazon.product.adapter.ElasticsearchHitAdapter;
import com.team14.amazon.product.dto.ProductSearchResultDTO;
import com.team14.amazon.product.model.elasticsearch.ProductSearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductFullTextSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductFullTextSearchService.class);

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchHitAdapter hitAdapter = new ElasticsearchHitAdapter();

    public ProductFullTextSearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Cacheable(
            value = "product-service::S2-F10",
            key = "#query + '_' + T(String).valueOf(#category) + '_' + T(String).valueOf(#brand) + '_' + T(String).valueOf(#status) + '_' + T(String).valueOf(#minPrice) + '_' + T(String).valueOf(#maxPrice) + '_' + T(String).valueOf(#minRating) + '_' + T(String).valueOf(#maxRating)"
    )
    public List<ProductSearchResultDTO> fullTextSearch(
            String query,
            String category,
            String brand,
            String status,
            Double minPrice,
            Double maxPrice,
            Double minRating,
            Double maxRating) {

        try {

            List<Query> mustClauses = new ArrayList<>();
            List<Query> filterClauses = new ArrayList<>();

            // =========================
            // FULL TEXT SEARCH
            // =========================
            if (query != null && !query.isBlank()) {
                mustClauses.add(Query.of(q -> q.multiMatch(m -> m
                        .query(query)
                        .fields("name", "description")
                        .type(TextQueryType.BestFields)
                )));
            }

            // =========================
            // TERM FILTERS
            // =========================
            if (category != null && !category.isBlank()) {
                filterClauses.add(Query.of(q -> q.term(t -> t
                        .field("category")
                        .value(FieldValue.of(category))
                )));
            }

            if (brand != null && !brand.isBlank()) {
                filterClauses.add(Query.of(q -> q.term(t -> t
                        .field("brand")
                        .value(FieldValue.of(brand))
                )));
            }

            if (status != null && !status.isBlank()) {
                filterClauses.add(Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(FieldValue.of(status))
                )));
            }

            // =========================
            // PRICE RANGE (FIXED TYPES)
            // =========================
            if (minPrice != null || maxPrice != null) {
                filterClauses.add(Query.of(q -> q.range(r -> r
                        .number(n -> {
                            n.field("price");
                            if (minPrice != null) n.gte(minPrice);
                            if (maxPrice != null) n.lte(maxPrice);
                            return n;
                        })
                )));
            }

            // =========================
            // RATING RANGE (FIXED TYPES)
            // =========================
            if (minRating != null || maxRating != null) {
                filterClauses.add(Query.of(q -> q.range(r -> r
                        .number(n -> {
                            n.field("rating");
                            if (minRating != null) n.gte(minRating);
                            if (maxRating != null) n.lte(maxRating);
                            return n;
                        })
                )));
            }

            // =========================
            // EXECUTE SEARCH
            // =========================
            SearchResponse<ProductSearchDocument> response =
                    elasticsearchClient.search(s -> s
                                    .index("products")
                                    .query(q -> q.bool(b -> {
                                        if (!mustClauses.isEmpty()) b.must(mustClauses);
                                        if (!filterClauses.isEmpty()) b.filter(filterClauses);
                                        return b;
                                    }))
                                    .size(100),
                            ProductSearchDocument.class
                    );

            // =========================
            // MAP RESULTS
            // =========================
            List<ProductSearchResultDTO> results = new ArrayList<>();

            for (Hit<ProductSearchDocument> hit : response.hits().hits()) {
                ProductSearchDocument doc = hitAdapter.adapt(hit);
                if (doc != null) {
                    results.add(toDTO(doc));
                }
            }

            return results;

        } catch (Exception e) {
            log.warn("Elasticsearch full-text search failed", e);
            return new ArrayList<>();
        }
    }

    private ProductSearchResultDTO toDTO(ProductSearchDocument doc) {
        ProductSearchResultDTO dto = new ProductSearchResultDTO();
        dto.setId(doc.getId());
        dto.setName(doc.getName());
        dto.setDescription(doc.getDescription());
        dto.setCategory(doc.getCategory());
        dto.setBrand(doc.getBrand());
        dto.setPrice(doc.getPrice());
        dto.setStockQuantity(doc.getStockQuantity());
        dto.setRating(doc.getRating());
        dto.setStatus(doc.getStatus());
        return dto;
    }
}