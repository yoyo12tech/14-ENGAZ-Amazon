package com.team14.amazon.product.service;
import com.team14.amazon.product.dto.LowStockAlertDTO;
import com.team14.amazon.product.dto.ProductSalesDTO;
import com.team14.amazon.product.dto.TopProductDTO;
import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.repository.ProductReviewRepository;
import com.team14.amazon.product.repository.ProductRepository;
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

    public ProductService(ProductRepository productRepository,
                          ProductReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.reviewRepository  = reviewRepository;
    }

    //Below are the main crud methods for the product
    //which are :findAll,findById,update,delete,create
    //In productRepository you will find extra things ,that may be used later

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

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
        return productRepository.save(product);
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

        return productRepository.save(product);
    }




    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
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

            return productRepository.save(existingProduct);
        });
    }
    // S2-F2: JSONB partial update — merge incoming specs into existing map
    public Optional<Product> updateSpecifications(Long id, Map<String, Object> incomingSpecs) {
        return productRepository.findById(id).map(product -> {
            Map<String, Object> existing = product.getSpecifications();
            if (existing == null) {
                existing = new HashMap<>();
            }
            existing.putAll(incomingSpecs); // add new keys, overwrite matching keys
            product.setSpecifications(existing);
            return productRepository.save(product);
        });
    }





//s2-f3
    public ProductSalesDTO getProductSalesSummary(Long id, LocalDate startDate, LocalDate endDate) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));

        LocalDate exclusiveEnd = endDate.plusDays(1);

        // 1. Get the list of results
        List<Object[]> results = productRepository.getProductSalesSummary(id, startDate, exclusiveEnd);

        // 2. Handle empty results (e.g., if the left join has no matches)
        if (results == null || results.isEmpty()) {
            return new ProductSalesDTO(id, product.getName(), 0L, 0.0, 0.0);
        }

        // 3. Extract the first row (the Object array)
        Object[] row = results.get(0);

        // 4. Map columns to DTO with proper Number casting
        return new ProductSalesDTO(
                ((Number) row[0]).longValue(),    // productId
                (String)  row[1],                 // name
                ((Number) row[2]).longValue(),    // totalUnitsSold
                ((Number) row[3]).doubleValue(),  // totalRevenue
                ((Number) row[4]).doubleValue()   // averageSellingPrice
        );
    }
    // S2-F1: Search products by optional category and price range
    // Validation in service layer (Lab 4 slide 46 — business logic belongs here)
    public List<Product> searchProducts(com.team14.amazon.product.model.ProductCategory category, Double minPrice, Double maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        return productRepository.searchProducts(category, minPrice, maxPrice);
    }

    public List<Product> searchBySpecification(String key, String value, ProductStatus status) {
        return productRepository.findBySpecification(key, value, status);
    }

    public List<TopProductDTO> getTopRatedProducts(int limit) {
        int normalizedLimit = limit > 0 ? limit : 10;
        List<Object[]> rows = productRepository.findTopRatedProducts(org.springframework.data.domain.PageRequest.of(0, normalizedLimit));

        List<TopProductDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new TopProductDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).doubleValue(),
                    ((Number) row[3]).longValue()
            ));
        }
        return result;
    }



    // S2-F9: Low Stock Alert Report
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

            result.add(new LowStockAlertDTO(
                    p.getId(),
                    p.getName(),
                    p.getStockQuantity(),
                    p.getCategory(),
                    recentSalesCount,
                    hasNegativeReviews
            ));
        }

        return result;
    }


}