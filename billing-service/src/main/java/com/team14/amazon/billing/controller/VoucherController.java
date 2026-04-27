package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.cache.BillingCacheInvalidationService;
import com.team14.amazon.billing.model.Voucher;

import com.team14.amazon.billing.service.VoucherService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private final VoucherService voucherService;
    private final BillingCacheInvalidationService cacheInvalidationService;

    public VoucherController(VoucherService voucherService,
                             BillingCacheInvalidationService cacheInvalidationService) {
        this.voucherService = voucherService;
        this.cacheInvalidationService = cacheInvalidationService;
    }


    @PostMapping
    public Voucher createVoucher(@RequestBody Voucher voucher){
        Voucher created = voucherService.createVoucher(voucher);
        cacheInvalidationService.evictVoucherCaches(created.getId());
        return created;
    }

    @GetMapping("/{id}")
    public Voucher getVoucherById(@PathVariable Long id){
        return voucherService.findVoucherById(id);
    }

    @GetMapping
    public List<Voucher> getAllVouchers(){
        return voucherService.findAllVouchers();
    }

    @PutMapping("/{id}")
    public Voucher updateVoucher(@PathVariable Long id, @RequestBody Voucher voucher) {
        voucher.setId(id);
        Voucher updated = voucherService.updateVoucher(voucher);
        cacheInvalidationService.evictVoucherCaches(updated.getId());
        return updated;
    }

    @DeleteMapping("/{id}")
    public void deleteVoucher(@PathVariable  Long id){
        voucherService.deleteVoucher(id);
        cacheInvalidationService.evictVoucherCaches(id);
    }

    @GetMapping("/search")
    public Voucher findVoucherByCode(@RequestParam String code) {
        return voucherService.findVoucherByCode(code);
    }

    @GetMapping("/active")
    public List<Voucher> searchForActiveVouchers(){
        return voucherService.searchForActiveVouchers();
    }
}
