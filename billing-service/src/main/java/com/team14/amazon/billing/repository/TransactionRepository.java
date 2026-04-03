package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByOrderId(Long orderId);
    Transaction findByUserId(Long userId);
    List<Transaction> findByStatus(Status status);
    List<Transaction> findByMethod(Method method);
}
