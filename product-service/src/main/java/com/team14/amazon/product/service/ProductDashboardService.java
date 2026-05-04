package com.team14.amazon.product.service;

import com.team14.amazon.product.dto.ProductCatalogDashboardDTO;
import com.team14.amazon.product.observer.EntityObserver;
import com.team14.amazon.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductDashboardService {

    private final ProductRepository productRepository;
    private final EntityObserver mongoEventLogger;

    public ProductDashboardService(
            ProductRepository productRepository,
            @Qualifier("mongoEventLogger") EntityObserver mongoEventLogger
    ) {
        this.productRepository = productRepository;
        this.mongoEventLogger = mongoEventLogger;
    }

    @Cacheable(value = "product-service::S2-F12", key = "'dashboard'")
    public ProductCatalogDashboardDTO getDashboard() {

        long totalProducts = productRepository.countAllProducts();
        long outOfStockCount = productRepository.countOutOfStock();
        double averageRating = productRepository.averageRating();
        double averagePrice = productRepository.averagePrice();
        long lowStockCount = productRepository.countLowStock();

        List<Object[]> catRows = productRepository.categoryDistribution();
        Map<String, Long> categoryDistribution = new HashMap<>();

        for (Object[] row : catRows) {
            String category = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count = ((Number) row[1]).longValue();
            categoryDistribution.put(category, count);
        }

        return ProductCatalogDashboardDTO.builder()
                .totalProducts(totalProducts)
                .outOfStockCount(outOfStockCount)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .averagePrice(Math.round(averagePrice * 100.0) / 100.0)
                .lowStockCount(lowStockCount)
                .categoryDistribution(categoryDistribution)
                .build();
    }

    public void logDashboardViewed() {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("productId", 0L);
            payload.put("timestamp", LocalDateTime.now());
            payload.put("details", Map.of("action", "DASHBOARD_VIEWED"));

            mongoEventLogger.onEvent("DASHBOARD_VIEWED", payload);
        } catch (Exception ignored) {
        }
    }
}