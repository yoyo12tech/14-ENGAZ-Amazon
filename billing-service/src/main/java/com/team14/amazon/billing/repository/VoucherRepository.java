package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Voucher getByCode(String code);
    List<Voucher> searchForActiveVouchers();
}
