package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
