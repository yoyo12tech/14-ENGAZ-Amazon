package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.TransactionVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionVoucherRepository extends JpaRepository<TransactionVoucher, Long> {
    boolean existsByTransactionIdAndVoucherId(Long transactionId, Long voucherId);
    List<TransactionVoucher> findByTransactionId(Long transactionId);
}
