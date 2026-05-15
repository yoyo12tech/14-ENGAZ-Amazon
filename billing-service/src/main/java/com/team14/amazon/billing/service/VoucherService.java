package com.team14.amazon.billing.service;

import com.team14.amazon.billing.cache.BillingCacheNames;
import com.team14.amazon.billing.model.Voucher;
import com.team14.amazon.billing.repository.VoucherRepository;
import com.team14.amazon.billing.cache.BillingCacheInvalidationService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final BillingCacheInvalidationService cacheInvalidationService;

    public VoucherService(VoucherRepository voucherRepository, BillingCacheInvalidationService cacheInvalidationService) {
        this.voucherRepository = voucherRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    public Voucher createVoucher(Voucher voucher){
        Voucher created = voucherRepository.save(voucher);
        cacheInvalidationService.evictVoucherCaches(created.getId());
        return created;
    }

    @Cacheable(cacheNames = BillingCacheNames.VOUCHER, key = "T(com.team14.amazon.billing.cache.BillingCacheNames).entityKey(#id)")
    public Voucher findVoucherById(Long id){
        return voucherRepository.findById(id).orElseThrow();
    }

    public List<Voucher> findAllVouchers(){
        return voucherRepository.findAll();
    }

    public Voucher updateVoucher(Voucher voucher) {
        // Check if it exists to throw an error if missing, then save the NEW data
        if (!voucherRepository.existsById(voucher.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        Voucher saved = voucherRepository.save(voucher);
        cacheInvalidationService.evictVoucherCaches(saved.getId());
        return saved;
    }

    public void deleteVoucher(Long id){
        voucherRepository.deleteById(id);
        cacheInvalidationService.evictVoucherCaches(id);
    }

    public Voucher findVoucherByCode(String code){
        return voucherRepository.getByCode(code);
    }

    public List<Voucher> searchForActiveVouchers(){
        return voucherRepository.searchForActiveVouchers();
    }
}
