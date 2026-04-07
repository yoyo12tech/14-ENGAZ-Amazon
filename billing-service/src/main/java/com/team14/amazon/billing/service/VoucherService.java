package com.team14.amazon.billing.service;

import com.team14.amazon.billing.model.DiscountType;
import com.team14.amazon.billing.model.Voucher;
import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import com.team14.amazon.billing.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final TransactionVoucherRepository transactionVoucherRepository;

    // Add to your existing constructor
    public VoucherService(VoucherRepository voucherRepository,
                          TransactionVoucherRepository transactionVoucherRepository) {
        this.voucherRepository = voucherRepository;
        this.transactionVoucherRepository = transactionVoucherRepository;
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

    public Voucher updateVoucher(Voucher voucher){
        Voucher toUpdate = voucherRepository.findById(voucher.getId()).orElseThrow();
        return voucherRepository.save(toUpdate);
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
    public List<VoucherUsageDTO> getTopUsedVouchers(int limit) {

        // Step 1 & 2 - Query TransactionVoucher table, group by voucher,
        //              compute totalDiscountGiven, order by timesUsed DESC, limit to n
        List<Object[]> results = transactionVoucherRepository.findTopUsedVouchers(limit);

        // Step 3 - Map each result row to a VoucherUsageDTO
        return results.stream()
                .map(row -> new VoucherUsageDTO(
                        (Long) row[0],                                          // voucherId
                        (String) row[1],                                        // code
                        (DiscountType) row[2],                                  // discountType
                        (Double) row[3],                                        // discountValue
                        (Integer) row[4],                                       // timesUsed (currentUses)
                        (Double) row[5],                                        // totalDiscountGiven (SUM)
                        (Boolean) row[6],                                       // active
                        ((LocalDateTime) row[7]).isBefore(LocalDateTime.now())  // expired
                ))
                .toList();
    }
}
