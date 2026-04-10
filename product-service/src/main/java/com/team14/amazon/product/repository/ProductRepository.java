package com.team14.amazon.product.repository;

import java.util.List;
javautilListpository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.team14.amazon.product.model.Product;
import comtteam144amazon.product.model.Productzon.product.model.ProductStatus;
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

//If this product is linked to a PENDING order
    //then we return 1 which means we can't discontinue it
    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE oi.product_id = :productId
          AND o.status = 'PENDING'
    )
""", nativeQuery = true)
    boolean existsPendingOrders(@Param("productId") Long productId);



    
}