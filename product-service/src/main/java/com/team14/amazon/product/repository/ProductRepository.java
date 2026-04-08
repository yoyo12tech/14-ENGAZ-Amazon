package com.team14.amazon.product.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
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



  @Query(value = """
        SELECT * FROM products
        WHERE specifications ->> :key = :value
        AND (CAST(:status AS text) IS NULL OR CAST(status AS text) = CAST(:status AS text))
        """, nativeQuery = true)
    List<Product> findBySpecificationKeyValue(
        @Param("key") String key,
        @Param("value") String value,
        @Param("status") String status
    );


// S2-F6: Spring Data JPA handles the sorting automatically
    List<Product> findAllByOrderByRatingDesc(Pageable pageable);

    // S2-F4 FIX: Safely cast the ENUM to check for pending orders
    @Query(value = """
        SELECT COUNT(*) > 0
        FROM orders o 
        JOIN order_items oi ON o.id = oi.order_id 
        WHERE oi.product_id = :productId 
        AND CAST(o.status AS text) = 'PENDING'
        """, nativeQuery = true)
    boolean existsPendingOrdersForProduct(@Param("productId") Long productId);

    // S2-F6: Safely cast the ENUM to count total sales
    @Query(value = """
        SELECT COUNT(oi.id) 
        FROM orders o 
        JOIN order_items oi ON o.id = oi.order_id 
        WHERE oi.product_id = :productId 
        AND CAST(o.status AS text) = 'DELIVERED'
        """, nativeQuery = true)
    Long countTotalSalesForProduct(@Param("productId") Long productId);
}