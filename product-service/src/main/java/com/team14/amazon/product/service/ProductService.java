package com.team14.amazon.product.service;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import com.team14.amazon.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import com.team14.amazon.product.dto.ProductSalesDTO;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.team14.amazon.product.dto.ProductSalesDTO;
import java.time.LocalDate;
import java.util.NoSuchElementException;

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

}