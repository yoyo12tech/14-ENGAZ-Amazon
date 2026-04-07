package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.TransactionVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionVoucherRepository extends JpaRepository<TransactionVoucher, Long> {

    @Query("""
        SELECT tv.voucher.id,
               tv.voucher.code,
               tv.voucher.discountType,
               tv.voucher.discountValue,
               tv.voucher.currentUses,
               SUM(tv.discountApplied),
               tv.voucher.active,
               tv.voucher.expiryDate
        FROM TransactionVoucher tv
        GROUP BY tv.voucher.id,
                 tv.voucher.code,
                 tv.voucher.discountType,
                 tv.voucher.discountValue,
                 tv.voucher.currentUses,
                 tv.voucher.active,
                 tv.voucher.expiryDate
        ORDER BY tv.voucher.currentUses DESC
        LIMIT :limit
        """)
    List<Object[]> findTopUsedVouchers(@Param("limit") int limit);
}
