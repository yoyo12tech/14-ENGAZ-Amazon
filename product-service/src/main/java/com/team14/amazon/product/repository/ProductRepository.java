package com.team14.amazon.product.repository;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);
    List<Product> findByStatus(ProductStatus status);


    List<Product> findByBrand(String brand);

    // S2-F3: Change return type to List<Object[]> to avoid the casting error
// S2-F3: Fixed Native SQL to correctly filter by date and status
    @Query(value = """
    SELECT
        p.id                                                        AS productId,
        p.name                                                      AS name,
        COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END), 0) 
                                                                    AS totalUnitsSold,
        COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN (oi.quantity * oi.price_at_purchase) ELSE 0.0 END), 0.0) 
                                                                    AS totalRevenue,
        COALESCE(AVG(CASE WHEN o.id IS NOT NULL THEN oi.price_at_purchase ELSE NULL END), 0.0) 
                                                                    AS averageSellingPrice
    FROM products p
    LEFT JOIN order_items oi ON oi.product_id = p.id
    LEFT JOIN orders o       ON o.id = oi.order_id
                            AND o.status = 'DELIVERED'
                            AND o.ordered_at >= CAST(:startDate AS timestamp)
                            AND o.ordered_at <  CAST(:endDate   AS timestamp)
    WHERE p.id = :productId
    GROUP BY p.id, p.name
    """, nativeQuery = true)
    List<Object[]> getProductSalesSummary(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );

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
}