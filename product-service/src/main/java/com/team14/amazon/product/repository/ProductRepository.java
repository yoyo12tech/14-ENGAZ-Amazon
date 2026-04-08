package com.team14.amazon.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByBrand(String brand);

    // S2-F1: Search by optional category and price range, ordered by price ascending
    // JPQL — uses entity field names (Lab 4 slide 43-44)
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "ORDER BY p.price ASC")
    List<Product> searchProducts(
            @Param("category") String category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );


   // In ProductRepository.java
   @Query(value = """
    SELECT COUNT(*) > 0
    FROM orders o
    JOIN order_items oi ON o.id = oi.order_id
    WHERE oi.product_id = :productId
    AND o.status = 'PENDING'
    """, nativeQuery = true)
boolean existsPendingOrdersForProduct(@Param("productId") Long productId); 
}