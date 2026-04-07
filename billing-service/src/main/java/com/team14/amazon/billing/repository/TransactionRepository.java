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

    @Query(value = "SELECT COUNT(id) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean checkUserExists(@Param("userId") Long userId);

    @Query("SELECT t.method, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status GROUP BY t.method")
    List<Object[]> getCompletedTransactionSummaryByUser(@Param("userId") Long userId, @Param("status") Status status);
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:#{#status == null} = true OR t.status = :status) AND " +
            "(:#{#startDate == null} = true OR t.createdAt >= :startDate) AND " +
            "(:#{#endDate == null} = true OR t.createdAt <= :endDate) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByFilters(
            @Param("status") Status status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
