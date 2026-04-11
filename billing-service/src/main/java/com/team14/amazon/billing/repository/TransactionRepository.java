package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByOrderId(Long orderId);
    Transaction findByUserId(Long userId);
    List<Transaction> findByStatus(Status status);
    List<Transaction> findByMethod(Method method);
    List<Transaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    boolean existsByUserId(Long userId);

    @Query("SELECT t.method, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status GROUP BY t.method")
    List<Object[]> getCompletedTransactionSummaryByUser(@Param("userId") Long userId, @Param("status") Status status);
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR t.createdAt <= :endDate) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByFilters(
            @Param("status") Status status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = "SELECT status FROM orders WHERE id = :orderId", nativeQuery = true)
    String findOrderStatusByOrderId(@Param("orderId") Long orderId);
}
