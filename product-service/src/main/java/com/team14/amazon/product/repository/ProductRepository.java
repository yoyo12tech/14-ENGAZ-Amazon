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

  // S2-F1: Search by optional category and price range, ordered by price
  // ascending
  // JPQL — uses entity field names (Lab 4 slide 43-44)
  @Query("SELECT p FROM Product p WHERE " +
      "(:#{#category == null} = true OR p.category = :category) AND " +
      "(:#{#minPrice == null} = true OR p.price >= :minPrice) AND " +
      "(:#{#maxPrice == null} = true OR p.price <= :maxPrice) " +
      "ORDER BY p.price ASC")
  List<Product> searchProducts(
      @Param("category") com.team14.amazon.product.model.ProductCategory category,
      @Param("minPrice") Double minPrice,
      @Param("maxPrice") Double maxPrice);

  // S2-F9: Find ACTIVE products whose stockQuantity is at or below the threshold
  @Query(value = "SELECT * FROM products p " +
      "WHERE p.stock_quantity <= :threshold " +
      "AND CAST(p.status AS text) = 'ACTIVE'", nativeQuery = true)
  List<Product> findActiveProductsWithStockAtOrBelow(@Param("threshold") int threshold);

  @Query("""
      SELECT p
      FROM Product p
      WHERE LOWER(p.brand) = LOWER(:brand)
        AND (:status IS NULL OR p.status = :status)
      """)
  List<Product> findByBrandAndStatus(
      @Param("brand") String brand,
      @Param("status") ProductStatus status);

  @Query(value = "SELECT * FROM products p" +
      " WHERE p.specifications->>:key = :value" +
      " AND (CAST(:status AS text) = 'ALL' OR CAST(p.status AS text) = CAST(:status AS text))", nativeQuery = true)
  List<Product> findBySpecification(
      @Param("key") String key,
      @Param("value") String value,
      @Param("status") String status);

  @Query("SELECT p FROM Product p ORDER BY p.rating DESC")
  List<Product> findTopRatedCandidates(org.springframework.data.domain.Pageable pageable);

  @Query("SELECT COUNT(p) FROM Product p")
  long countAllProducts();

  @Query(value = "SELECT COUNT(*) FROM products WHERE CAST(status AS text) = 'DISCONTINUED' OR CAST(status AS text) = 'OUT_OF_STOCK'", nativeQuery = true)
  long countOutOfStock();

  @Query("SELECT COALESCE(AVG(p.rating), 0.0) FROM Product p WHERE p.totalRatings > 0")
  double averageRating();

  @Query("SELECT p.category, COUNT(p) FROM Product p GROUP BY p.category")
  List<Object[]> categoryDistribution();

  @Query("SELECT COALESCE(AVG(p.price), 0.0) FROM Product p")
  double averagePrice();

  @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity <= 10 AND p.status = com.team14.amazon.product.model.ProductStatus.ACTIVE")
  long countLowStock();

  @org.springframework.data.jpa.repository.Modifying
  @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :quantity WHERE id = :productId AND stock_quantity >= :quantity AND CAST(status AS text) != 'OUT_OF_STOCK'", nativeQuery = true)
  int deductStockIfAvailable(@Param("productId") Long productId, @Param("quantity") Integer quantity);

  @org.springframework.data.jpa.repository.Modifying
  @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :quantity WHERE id = :productId", nativeQuery = true)
  int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

}
