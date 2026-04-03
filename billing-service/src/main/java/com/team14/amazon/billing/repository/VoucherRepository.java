package com.team14.amazon.billing.repository;

import com.team14.amazon.billing.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Voucher getByCode(String code);
    @Query("SELECT v FROM Voucher v WHERE v.active = true")
    List<Voucher> searchForActiveVouchers();
}
