package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.model.TransactionVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface TransactionVoucherRepository extends JpaRepository<TransactionVoucher, Long> {



    @Query("SELECT new com.team14.amazon.billing.dto.VoucherUsageDTO(" +
            "v.id, v.code, v.discountType, v.discountValue, v.currentUses, " +
            "SUM(tv.discountApplied), v.active, " +
            "(CASE WHEN v.expiryDate < CURRENT_TIMESTAMP THEN true ELSE false END)) " +
            "FROM TransactionVoucher tv " +
            "JOIN tv.voucher v " +
            "GROUP BY v.id, v.code, v.discountType, v.discountValue, v.currentUses, v.active, v.expiryDate " +
            "ORDER BY v.currentUses DESC")
    List<VoucherUsageDTO> findTopUsedVouchers(Pageable pageable);
    boolean existsByTransactionIdAndVoucherId(Long transactionId, Long voucherId);
    List<TransactionVoucher> findByTransactionId(Long transactionId);
}
