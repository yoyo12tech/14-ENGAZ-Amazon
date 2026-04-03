package com.team14.amazon.billing.controller;

import com.team14.amazon.billing.model.Voucher;

import com.team14.amazon.billing.service.VoucherService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voucher")
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }


    @PostMapping
    public Voucher createVoucher(@RequestBody Voucher voucher){
        return voucherService.createVoucher(voucher);
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
    public Voucher updateVoucher(Voucher voucher){
        return voucherService.updateVoucher(voucher);
    }

    @DeleteMapping("/{id}")
    public void deleteVoucher(Long id){
        voucherService.deleteVoucher(id);
    }

    @GetMapping("/search")
    public Voucher findVoucherByCode(String code){
        return voucherService.findVoucherByCode(code);
    }

    @GetMapping("/active")
    public List<Voucher> searchForActiveVouchers(){
        return voucherService.searchForActiveVouchers();
    }
}
