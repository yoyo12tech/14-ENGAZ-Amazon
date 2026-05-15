package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Transaction t SET t.status = :newStatus WHERE t.id = :id AND t.status = :oldStatus")
    int updateStatusAtomically(@Param("id") Long id,
                               @Param("oldStatus") TransactionStatus oldStatus,
                               @Param("newStatus") TransactionStatus newStatus);

    Transaction findByOrderId(Long orderId);

    Transaction findByUserId(Long userId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByMethod(TransactionMethod method);

    List<Transaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end AND t.status IN :statuses")
    List<Transaction> findByCreatedAtBetweenAndStatusIn(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end,
                                                      @Param("statuses") Collection<TransactionStatus> statuses);

    boolean existsByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :start AND :end AND t.status = com.team14.amazon.billing.model.TransactionStatus.COMPLETED")
    BigDecimal getUserTransactionTotal(@Param("userId") Long userId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :start AND :end")
    long getUserOrderCount(@Param("userId") Long userId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    @Query("SELECT t.method, COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status GROUP BY t.method")
    List<Object[]> getCompletedTransactionSummaryByUser(@Param("userId") Long userId,
                                                        @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE "
            + "(:status IS NULL OR t.status = :status) AND "
            + "(:startDate IS NULL OR t.createdAt >= :startDate) AND "
            + "(:endDate IS NULL OR t.createdAt <= :endDate) "
            + "ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByFilters(
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<Transaction> findAllByOrderId(Long orderId);
}
