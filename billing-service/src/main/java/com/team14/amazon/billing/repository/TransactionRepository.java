package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.model.Transaction;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByOrderId(Long orderId);
    Transaction findByUserId(Long userId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByMethod(TransactionMethod method);
    List<Transaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    boolean existsByUserId(Long userId);

    // Sum of COMPLETED transaction amounts for a user in a date range
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :start AND :end AND t.status = com.team14.amazon.billing.model.TransactionStatus.COMPLETED")
    BigDecimal getUserTransactionTotal(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Count of all transactions (1 per order) for a user in a date range
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :start AND :end")
    long getUserOrderCount(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t.method, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status GROUP BY t.method")
    List<Object[]> getCompletedTransactionSummaryByUser(@Param("userId") Long userId, @Param("status") TransactionStatus status);
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR t.createdAt <= :endDate) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByFilters(
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT status FROM orders WHERE id = :orderId", nativeQuery = true)
    String findOrderStatusByOrderId(@Param("orderId") Long orderId);
    
    @Query(value = "SELECT oi.id, oi.product_id, oi.quantity, oi.price_at_purchase FROM order_items oi WHERE oi.order_id = :orderId", nativeQuery = true)
    List<Object[]> findOrderItemsByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT COUNT(*) FROM order_items WHERE id IN :itemIds AND order_id = :orderId", nativeQuery = true)
    Long countItemsBelongingToOrder(@Param("itemIds") List<Long> itemIds, @Param("orderId") Long orderId);

    @Query(value = "SELECT id FROM shipments WHERE order_id = :orderId", nativeQuery = true)
    List<Long> findShipmentIdsByOrderId(@Param("orderId") Long orderId);

    @Query(value = "SELECT p.category AS category, " +
            "oi.id AS orderItemId, " +
            "oi.quantity AS quantity, " +
            "oi.price_at_purchase AS priceAtPurchase, " +
            "t.status AS transactionStatus, " +
            "CAST(t.transaction_details AS text) AS transactionDetails, " +
            "t.id AS transactionId " +
            "FROM transactions t " +
            "JOIN orders o ON t.order_id = o.id " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "JOIN products p ON oi.product_id = p.id " +
            "WHERE o.ordered_at BETWEEN :startDate AND :endDate " +
            "AND t.status IN ('COMPLETED', 'REFUNDED')", nativeQuery = true)
    List<CategoryRevenueProjection> findCategoryRevenueRawData(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
