package com.team14.amazon.product.repository;

import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // S2-F9: Find ACTIVE products whose stockQuantity is at or below the threshold
    @Query(value = "SELECT * FROM products p " +
            "WHERE p.stock_quantity <= :threshold " +
            "AND p.status = 'ACTIVE'::product_status",
            nativeQuery = true)
    List<Product> findActiveProductsWithStockAtOrBelow(@Param("threshold") int threshold);

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.product_id = :productId
              AND o.status IN ('PENDING', 'CONFIRMED')
            """, nativeQuery = true)
    boolean hasActiveOrders(@Param("productId") Long productId);

    @Query(value = """
            SELECT *
            FROM products p
            WHERE p.specifications->>:key = :value
              AND (:status IS NULL OR p.status = CAST(:status AS product_status))
            """, nativeQuery = true)
    List<Product> findBySpecification(
            @Param("key") String key,
            @Param("value") String value,
            @Param("status") String status
    );

    @Query(value = """
            SELECT p.id,
                   p.name,
                   p.rating,
                   COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN oi.quantity ELSE 0 END), 0) AS totalSales
            FROM products p
            LEFT JOIN order_items oi ON oi.product_id = p.id
            LEFT JOIN orders o ON o.id = oi.order_id
            GROUP BY p.id, p.name, p.rating
            ORDER BY p.rating DESC, totalSales DESC, p.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopRatedProducts(@Param("limit") int limit);

    @Query(value = """
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.product_id = :productId
              AND o.status = 'DELIVERED'
              AND o.ordered_at >= :since
            """, nativeQuery = true)
    long countRecentDeliveredSales(
            @Param("productId") Long productId,
            @Param("since") LocalDateTime since
    );
}