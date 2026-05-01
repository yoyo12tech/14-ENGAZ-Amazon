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
import com.team14.amazon.product.repository.ProductReviewRepository;
import com.team14.amazon.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final EntityObserver mongoEventLogger;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final ObjectArrayProductSalesAdapter salesAdapter = new ObjectArrayProductSalesAdapter();
    private final ObjectArrayTopProductAdapter topProductAdapter = new ObjectArrayTopProductAdapter();

    public ProductService(ProductRepository productRepository,
                          ProductReviewRepository reviewRepository,
                          CacheInvalidationService cacheInvalidationService,
                          @Qualifier("mongoEventLogger") EntityObserver mongoEventLogger) {
        this.productRepository = productRepository;
        this.reviewRepository  = reviewRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.mongoEventLogger = mongoEventLogger;
    }

    @PostConstruct
    public void init() {
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    private Map<String, Object> productPayload(Long productId, Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("details", details != null ? details : Map.of());
        return payload;
    }

    //Below are the main crud methods for the product
    //which are :findAll,findById,update,delete,create
    //In productRepository you will find extra things ,that may be used later

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Cacheable(value = "product-service::product", key = "#id")
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product discontinueProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));

        if (productRepository.hasActiveOrders(id)) {
            throw new IllegalStateException("Cannot discontinue product with pending or confirmed orders.");
        }

        product.setStatus(ProductStatus.INACTIVE);
        Product saved = productRepository.save(product);
        cacheInvalidationService.invalidateProductCaches(id);
        notifyObservers("STATUS_CHANGED", productPayload(saved.getId(),
                Map.of("status", saved.getStatus().name(), "source", "CRUD")));
        return saved;
    }

    public Product createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }

        product.setRating(0.0);
        product.setTotalRatings(0);

        Product saved = productRepository.save(product);
        cacheInvalidationService.invalidateProductCaches(saved.getId());
        notifyObservers("PRODUCT_CREATED", productPayload(saved.getId(),
                Map.of("name", saved.getName(), "source", "CRUD")));
        return saved;
    }




    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            cacheInvalidationService.invalidateProductCaches(id);
            notifyObservers("PRODUCT_DELETED", productPayload(id, Map.of("source", "CRUD")));
            return true;
        }
        return false;
    }




    // --- Update an existing product ---
    public Optional<Product> updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id).map(existingProduct -> {
            existingProduct.setName(updatedProduct.getName());
            existingProduct.setDescription(updatedProduct.getDescription());
            existingProduct.setPrice(updatedProduct.getPrice());
            existingProduct.setCategory(updatedProduct.getCategory());
            existingProduct.setBrand(updatedProduct.getBrand());
            existingProduct.setStockQuantity(updatedProduct.getStockQuantity());
            existingProduct.setStatus(updatedProduct.getStatus());
            existingProduct.setSpecifications(updatedProduct.getSpecifications());

            Product saved = productRepository.save(existingProduct);
            cacheInvalidationService.invalidateProductCaches(id);
            notifyObservers("PRODUCT_UPDATED", productPayload(saved.getId(),
                    Map.of("name", saved.getName(), "source", "CRUD")));
            return saved;
        });
    }
    // S2-F2: JSONB partial update — merge incoming specs into existing map
    public Optional<Product> updateSpecifications(Long id, Map<String, Object> incomingSpecs) {
        return productRepository.findById(id).map(product -> {
            Map<String, Object> specsToMerge = incomingSpecs != null ? incomingSpecs : Map.of();
            Map<String, Object> existing = product.getSpecifications();
            if (existing == null) {
                existing = new HashMap<>();
            }
            existing.putAll(specsToMerge); // add new keys, overwrite matching keys
            product.setSpecifications(existing);
            Product saved = productRepository.save(product);
            cacheInvalidationService.invalidateProductCaches(id);
            notifyObservers("SPECIFICATIONS_UPDATED", productPayload(saved.getId(),
                    Map.of("updatedKeys", specsToMerge.keySet(), "source", "CRUD")));
            return saved;
        });
    }





//s2-f3
    @Cacheable(value = "product-service::S2-F3",
               key = "#id + '_' + #startDate + '_' + #endDate")
    public ProductSalesDTO getProductSalesSummary(Long id, LocalDate startDate, LocalDate endDate) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));

        LocalDate exclusiveEnd = endDate.plusDays(1);

        // 1. Get the list of results
        List<Object[]> results = productRepository.getProductSalesSummary(id, startDate, exclusiveEnd);

        // 2. Handle empty results (e.g., if the left join has no matches)
        if (results == null || results.isEmpty()) {
            return ProductSalesDTO.builder()
                    .productId(id)
                    .name(product.getName())
                    .totalUnitsSold(0L)
                    .totalRevenue(0.0)
                    .averageSellingPrice(0.0)
                    .build();
        }

        return salesAdapter.adapt(results.get(0));
    }
    // S2-F1: Search products by optional category and price range
    // Validation in service layer (Lab 4 slide 46 — business logic belongs here)
    @Cacheable(value = "product-service::S2-F1",
               key = "T(String).valueOf(#category) + '_' + T(String).valueOf(#minPrice) + '_' + T(String).valueOf(#maxPrice)")
    public List<Product> searchProducts(ProductCategory category, Double minPrice, Double maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        return productRepository.searchProducts(category, minPrice, maxPrice);
    }

    @Cacheable(value = "product-service::S2-F5",
               key = "#key + '_' + #value + '_' + T(String).valueOf(#status)")
    public List<Product> searchBySpecification(String key, String value, ProductStatus status) {
        return productRepository.findBySpecification(key, value, status);
    }

    @Cacheable(value = "product-service::S2-F6", key = "#limit")
    public List<TopProductDTO> getTopRatedProducts(int limit) {
        int normalizedLimit = limit > 0 ? limit : 10;
        List<Object[]> rows = productRepository.findTopRatedProducts(org.springframework.data.domain.PageRequest.of(0, normalizedLimit));

        List<TopProductDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(topProductAdapter.adapt(row));
        }
        return result;
    }



    // S2-F9: Low Stock Alert Report
    @Cacheable(value = "product-service::S2-F9", key = "#threshold")
    public List<LowStockAlertDTO> getLowStockAlerts(int threshold) {

        // 1. Query ACTIVE products with stock <= threshold
        List<Product> lowStockProducts =
                productRepository.findActiveProductsWithStockAtOrBelow(threshold);

        // 2. Cutoff date for "recent" sales (last 30 days)
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // 3. Build DTO list
        List<LowStockAlertDTO> result = new ArrayList<>();

        for (Product p : lowStockProducts) {

            // 4. Count recent DELIVERED sales with a dedicated query to avoid date-cast issues.
            long recentSalesCount = productRepository.countRecentDeliveredSales(p.getId(), since);

            // 5. Check for negative reviews (rating <= 2)
            boolean hasNegativeReviews = reviewRepository.hasNegativeReviews(p.getId());

            result.add(LowStockAlertDTO.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .currentStock(p.getStockQuantity())
                    .category(p.getCategory())
                    .recentSalesCount(recentSalesCount)
                    .hasNegativeReviews(hasNegativeReviews)
                    .build());
        }

        return result;
    }


}