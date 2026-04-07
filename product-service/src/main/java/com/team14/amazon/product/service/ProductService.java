package com.team14.amazon.product.service;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;


    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

    public Product createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }

        if (product.getRating() == null) {
            product.setRating(0.0);
        }

        if (product.getTotalRatings() == null) {
            product.setTotalRatings(0);
        }

        return productRepository.save(product);
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

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
    // S2-F1: Search products by optional category and price range
    // Validation in service layer (Lab 4 slide 46 — business logic belongs here)
    public List<Product> searchProducts(String category, Double minPrice, Double maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        return productRepository.searchProducts(category, minPrice, maxPrice);
    }

}