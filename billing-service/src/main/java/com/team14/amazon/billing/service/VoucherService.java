package com.team14.amazon.billing.service;

import com.team14.amazon.billing.model.Voucher;
import com.team14.amazon.billing.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    public Voucher createVoucher(Voucher voucher){
        return voucherRepository.save(voucher);
    }

    public Voucher findVoucherById(Long id){
        return voucherRepository.findById(id).orElseThrow();
    }

    public List<Voucher> findAllVouchers(){
        return voucherRepository.findAll();
    }

    public Voucher updateVoucher(Voucher voucher) {
        // Check if it exists to throw an error if missing, then save the NEW data
        if (!voucherRepository.existsById(voucher.getId())) {
            throw new java.util.NoSuchElementException("Voucher not found");
        }
        return voucherRepository.save(voucher);
    }

    public void deleteVoucher(Long id){
        voucherRepository.deleteById(id);
    }

    public Voucher findVoucherByCode(String code){
        return voucherRepository.getByCode(code);
    }

    public List<Voucher> searchForActiveVouchers(){
        return voucherRepository.searchForActiveVouchers();
    }
}
