package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.TransactionVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionVoucherRepository extends JpaRepository<TransactionVoucher, Long> {

    // Step 5 - Duplicate check
    boolean existsByTransactionIdAndVoucherId(Long transactionId, Long voucherId);
}
