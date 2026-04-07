package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByOrderId(Long orderId);
    Transaction findByUserId(Long userId);
    List<Transaction> findByStatus(Status status);
    List<Transaction> findByMethod(Method method);

    @Query(value = "SELECT COUNT(id) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean checkUserExists(@Param("userId") Long userId);

    @Query("SELECT t.method, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status GROUP BY t.method")
    List<Object[]> getCompletedTransactionSummaryByUser(@Param("userId") Long userId, @Param("status") Status status);
}
