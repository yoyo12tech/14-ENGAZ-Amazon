package com.team14.amazon.product.service;

import com.team14.amazon.product.adapter.ObjectArrayProductSalesAdapter;
import com.team14.amazon.product.adapter.ObjectArrayTopProductAdapter;
import com.team14.amazon.product.dto.LowStockAlertDTO;
import com.team14.amazon.product.dto.ProductSalesDTO;
import com.team14.amazon.product.dto.TopProductDTO;
import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductCategory;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.observer.EntityObserver;
import com.team14.amazon.product.repository.ProductRepository;
import com.team14.amazon.product.repository.ProductReviewRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.team14.amazon.product.messaging.publishers.ProductEventPublisher;
import com.team14.amazon.contracts.feign.OrderServiceClient;
import com.team14.amazon.contracts.dto.ProductSalesAggregateDTO;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final EntityObserver mongoEventLogger;
    private final ElasticsearchIndexService esIndexService;
    private final CacheInvalidationService cacheInvalidationService;
    private final OrderServiceClient orderServiceClient;
    private final ProductEventPublisher eventPublisher;
    private final List<EntityObserver> observers = new ArrayList<>();

    private final ObjectArrayProductSalesAdapter salesAdapter = new ObjectArrayProductSalesAdapter();
    private final ObjectArrayTopProductAdapter topProductAdapter = new ObjectArrayTopProductAdapter();

    public ProductService(ProductRepository productRepository,
                          ProductReviewRepository reviewRepository,
                          @Qualifier("mongoEventLogger") EntityObserver mongoEventLogger,
                          ElasticsearchIndexService esIndexService,
                          CacheInvalidationService cacheInvalidationService,
                          OrderServiceClient orderServiceClient,
                          ProductEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.reviewRepository  = reviewRepository;
        this.mongoEventLogger  = mongoEventLogger;
        this.esIndexService    = esIndexService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.orderServiceClient = orderServiceClient;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() { register(mongoEventLogger); }

    public void register(EntityObserver o)   { observers.add(o); }
    public void unregister(EntityObserver o) { observers.remove(o); }
    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver o : observers) o.onEvent(eventType, payload);
    }

    private Map<String, Object> productPayload(Long productId, Map<String, Object> details) {
        Map<String, Object> p = new HashMap<>();
        p.put("productId", productId);
        p.put("timestamp", LocalDateTime.now());
        p.put("details", details != null ? details : Map.of());
        return p;
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // @Cacheable on service — NOT on controller (controller wraps in ResponseEntity)
    @Cacheable(value = "product-service::product", key = "#id")
    public Optional<Product> getProductById(Long id) {
        MDC.put("productId", id.toString());
        try {
            return productRepository.findById(id);
        } finally {
            MDC.remove("productId");
        }
    }

    public List<com.team14.amazon.contracts.dto.ProductDTO> getProductsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return productRepository.findAllById(ids).stream()
                .map(p -> new com.team14.amazon.contracts.dto.ProductDTO(
                        p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                        p.getCategory() != null ? p.getCategory().name() : null,
                        p.getBrand(), p.getStockQuantity(),
                        p.getStatus() != null ? p.getStatus().name() : null,
                        p.getRating(), p.getSpecifications()))
                .toList();
    }

    public Product createProduct(Product product) {
        if (product.getStatus() == null)        product.setStatus(ProductStatus.ACTIVE);
        if (product.getStockQuantity() == null) product.setStockQuantity(0);
        if (product.getRating() == null)        product.setRating(0.0);
        if (product.getTotalRatings() == null)  product.setTotalRatings(0);

        Product saved = productRepository.save(product);
        MDC.put("productId", saved.getId().toString());
        try {
            // Observer
            notifyObservers("PRODUCT_CREATED", productPayload(saved.getId(),
                    Map.of("name", saved.getName(), "source", "CRUD")));

            // ES auto-index (source=auto_crud_create per M2 §10.2.2 step d)
            esIndexService.indexProduct(saved);

            // Invalidate feature caches (new product affects search, top-rated, low-stock)
            cacheInvalidationService.invalidateProductCaches(saved.getId());

            return saved;
        } finally {
            MDC.remove("productId");
        }
    }

    public Optional<Product> updateProduct(Long id, Product upd) {
        MDC.put("productId", id.toString());
        try {
            return productRepository.findById(id).map(e -> {
                e.setName(upd.getName()); e.setDescription(upd.getDescription());
                e.setPrice(upd.getPrice()); e.setCategory(upd.getCategory());
                e.setBrand(upd.getBrand()); e.setStockQuantity(upd.getStockQuantity());
                e.setStatus(upd.getStatus()); e.setSpecifications(upd.getSpecifications());
                if (upd.getRating() != null)       e.setRating(upd.getRating());
                if (upd.getTotalRatings() != null) e.setTotalRatings(upd.getTotalRatings());
                Product saved = productRepository.save(e);

                // Observer
                notifyObservers("PRODUCT_UPDATED", productPayload(saved.getId(),
                        Map.of("name", saved.getName(), "source", "CRUD")));

                // ES auto-index (source=auto_crud_update)
                esIndexService.indexProduct(saved);

                // Cache invalidation
                cacheInvalidationService.invalidateProductCaches(saved.getId());

                return saved;
            });
        } finally {
            MDC.remove("productId");
        }
    }

    public boolean deleteProduct(Long id) {
        MDC.put("productId", id.toString());
        try {
            if (productRepository.existsById(id)) {
                productRepository.deleteById(id);

                // Observer: PRODUCT_DELETED (not INDEXED) — M2 §10.2.2 step d
                notifyObservers("PRODUCT_DELETED", productPayload(id, Map.of("source", "CRUD")));

                // ES: remove from index
                esIndexService.deleteFromIndex(id);

                // Cache invalidation
                cacheInvalidationService.invalidateProductCaches(id);

                return true;
            }
            return false;
        } finally {
            MDC.remove("productId");
        }
    }

    public boolean deductStock(Long id, Integer quantity) {
        MDC.put("productId", id.toString());
        try {
            return productRepository.findById(id).map(p -> {
                if (p.getStockQuantity() < quantity) {
                    throw new IllegalArgumentException("Insufficient stock");
                }
                p.setStockQuantity(p.getStockQuantity() - quantity);
                Product saved = productRepository.save(p);
                notifyObservers("STOCK_UPDATED", productPayload(saved.getId(), Map.of("newStock", saved.getStockQuantity(), "source", "FEIGN_DEDUCT")));
                esIndexService.indexProduct(saved);
                cacheInvalidationService.invalidateProductCaches(saved.getId());
                return true;
            }).orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        } finally {
            MDC.remove("productId");
        }
    }

    public void restoreStock(Long id, Integer quantity) {
        MDC.put("productId", id.toString());
        try {
            productRepository.findById(id).ifPresentOrElse(p -> {
                p.setStockQuantity(p.getStockQuantity() + quantity);
                Product saved = productRepository.save(p);
                notifyObservers("STOCK_UPDATED", productPayload(saved.getId(), Map.of("newStock", saved.getStockQuantity(), "source", "FEIGN_RESTORE")));
                esIndexService.indexProduct(saved);
                cacheInvalidationService.invalidateProductCaches(saved.getId());
            }, () -> {
                throw new NoSuchElementException("Product not found: " + id);
            });
        } finally {
            MDC.remove("productId");
        }
    }

    // S2-F4
    public Product discontinueProduct(Long id) {
        MDC.put("productId", id.toString());
        try {
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
            if (orderServiceClient.getPendingOrderCountForProduct(id) > 0)
                throw new IllegalStateException("Product appears in pending orders");
            ProductStatus oldStatus = p.getStatus();
            p.setStatus(ProductStatus.INACTIVE);
            Product saved = productRepository.save(p);

            notifyObservers("STATUS_CHANGED", productPayload(saved.getId(),
                    Map.of("newStatus", "INACTIVE", "source", "CRUD")));
            
            eventPublisher.publishProductDiscontinued(saved.getId(), oldStatus.name(), "INACTIVE");

            esIndexService.indexProduct(saved); // status changed → re-index
            cacheInvalidationService.invalidateProductCaches(saved.getId());

            return saved;
        } finally {
            MDC.remove("productId");
        }
    }

    // S2-F2
    public Optional<Product> updateSpecifications(Long id, Map<String, Object> incoming) {
        MDC.put("productId", id.toString());
        try {
            return productRepository.findById(id).map(p -> {
                Map<String, Object> existing = p.getSpecifications();
                if (existing == null) existing = new HashMap<>();
                existing.putAll(incoming);
                p.setSpecifications(existing);
                Product saved = productRepository.save(p);

                notifyObservers("SPECIFICATIONS_UPDATED", productPayload(saved.getId(),
                        Map.of("keys", incoming.keySet(), "source", "CRUD")));
                esIndexService.indexProduct(saved);
                cacheInvalidationService.invalidateProductCaches(saved.getId());

                return saved;
            });
        } finally {
            MDC.remove("productId");
        }
    }

    // ── M1 feature GETs — @Cacheable on service layer ───────────────────────

    // S2-F1
    @Cacheable(value = "product-service::S2-F1",
            key = "T(String).valueOf(#category) + '_' + T(String).valueOf(#minPrice) + '_' + T(String).valueOf(#maxPrice)")
    public List<Product> searchProducts(ProductCategory category, Double minPrice, Double maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice)
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        return productRepository.searchProducts(category, minPrice, maxPrice);
    }

    // S2-F3
    @Cacheable(value = "product-service::S2-F3",
            key = "#id + '_' + #startDate + '_' + #endDate")
    public ProductSalesDTO getProductSalesSummary(Long id, LocalDate startDate, LocalDate endDate) {
        long startMs = System.currentTimeMillis();
        MDC.put("productId", id.toString());
        try {
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
            
            ProductSalesAggregateDTO agg = orderServiceClient.getProductSales(id, startDate.toString(), endDate.toString());
            
            ProductSalesDTO result;
            if (agg == null) {
                result = new ProductSalesDTO(id, p.getName(), 0L, 0.0, 0.0);
            } else {
                result = new ProductSalesDTO(id, p.getName(), agg.totalUnitsSold(), agg.totalRevenue(), agg.averageSellingPrice());
            }

            long elapsed = System.currentTimeMillis() - startMs;
            if (elapsed > 1000) {
                log.warn("Slow getProductSalesSummary took {}ms", elapsed);
            }
            return result;
        } finally {
            MDC.remove("productId");
        }
    }

    // S2-F5
    @Cacheable(value = "product-service::S2-F5",
            key = "#key + '_' + #value + '_' + T(String).valueOf(#status)")
    public List<Product> searchBySpecification(String key, String value, ProductStatus status) {
        String statusStr = (status != null) ? status.name() : "ALL";
        return productRepository.findBySpecification(key, value, statusStr);
    }

    // S2-F6
    @Cacheable(value = "product-service::S2-F6", key = "#limit")
    public List<TopProductDTO> getTopRatedProducts(int limit) {
        long startMs = System.currentTimeMillis();
        int n = limit > 0 ? limit : 10;
        List<Product> candidates = productRepository.findTopRatedCandidates(
                org.springframework.data.domain.PageRequest.of(0, n * 2));
                
        List<TopProductDTO> result = new ArrayList<>();
        for (Product p : candidates) {
            MDC.put("productId", p.getId().toString());
            try {
                long sales = orderServiceClient.getUnitsSold(p.getId());
                result.add(TopProductDTO.builder()
                        .productId(p.getId())
                        .name(p.getName())
                        .rating(p.getRating())
                        .totalSales(sales)
                        .build());
            } finally {
                MDC.remove("productId");
            }
        }
        
        result.sort((a, b) -> {
            int ratingCmp = Double.compare(b.getRating(), a.getRating());
            if (ratingCmp != 0) return ratingCmp;
            return Long.compare(b.getTotalSales(), a.getTotalSales());
        });
        
        if (result.size() > n) {
            result = result.subList(0, n);
        }

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > 1000) {
            log.warn("Slow getTopRatedProducts took {}ms", elapsed);
        }
        return result;
    }

    // S2-F9
    @Cacheable(value = "product-service::S2-F9", key = "#threshold")
    public List<LowStockAlertDTO> getLowStockAlerts(int threshold) {
        List<Product> low = productRepository.findActiveProductsWithStockAtOrBelow(threshold);
        List<LowStockAlertDTO> result = new ArrayList<>();
        for (Product p : low) {
            long sales = orderServiceClient.getRecentSalesCount(p.getId(), 30);
            boolean neg = reviewRepository.hasNegativeReviews(p.getId());
            result.add(LowStockAlertDTO.builder()
                    .productId(p.getId()).productName(p.getName())
                    .currentStock(p.getStockQuantity()).category(p.getCategory())
                    .recentSalesCount(sales).hasNegativeReviews(neg).build());
        }
        return result;
    }
}