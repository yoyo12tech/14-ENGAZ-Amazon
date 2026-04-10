package com.team14.amazon.product.service;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}



@Transactional
    public void discontinueProduct(Long id) {
        // 1. Find product – throws 404 if not found
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // 2. Check no pending orders contain this product — throws 400 if not
        // NOTE: You will need to inject an OrderService, OrderRepository, or FeignClient
        // depending on your architecture to actually perform this check.
        boolean hasPendingOrders = productRepository.existsPendingOrders(id);

        if (hasPendingOrders) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot discontinue product: It is currently part of a pending order."
            );
        }

        // 3. Update status and save
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }
