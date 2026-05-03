package com.team14.amazon.billing.service;

import com.team14.amazon.billing.cache.BillingCacheNames;
import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.model.TransactionVoucher;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import com.team14.amazon.billing.cache.BillingCacheInvalidationService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionVoucherService {

    private final TransactionVoucherRepository transactionVoucherRepository;
    private final BillingCacheInvalidationService cacheInvalidationService;

    public TransactionVoucherService(TransactionVoucherRepository transactionVoucherRepository, BillingCacheInvalidationService cacheInvalidationService) {
        this.transactionVoucherRepository = transactionVoucherRepository;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    // --- Basic CRUD Operations ---

    public TransactionVoucher create(TransactionVoucher transactionVoucher) {
        TransactionVoucher saved = transactionVoucherRepository.save(transactionVoucher);
        cacheInvalidationService.evictTransactionVoucherCaches(saved.getId());
        return saved;
    }

    @Cacheable(
            cacheNames = BillingCacheNames.TRANSACTION_VOUCHER,
            key = "T(com.team14.amazon.billing.cache.BillingCacheNames).entityKey(#id)",
            unless = "#result == null || #result.isEmpty()"
    )
    public Optional<TransactionVoucher> getById(Long id) {
        return transactionVoucherRepository.findById(id);
    }

    public List<TransactionVoucher> getAll() {
        return transactionVoucherRepository.findAll();
    }

    public Optional<TransactionVoucher> update(Long id, TransactionVoucher updatedDetails) {
        return transactionVoucherRepository.findById(id).map(existing -> {
            existing.setTransaction(updatedDetails.getTransaction());
            existing.setVoucher(updatedDetails.getVoucher());
            existing.setDiscountApplied(updatedDetails.getDiscountApplied());
            // Add any other fields that exist in your TransactionVoucher model
            TransactionVoucher saved = transactionVoucherRepository.save(existing);
            cacheInvalidationService.evictTransactionVoucherCaches(saved.getId());
            return saved;
        });
    }

    public boolean delete(Long id) {
        if (transactionVoucherRepository.existsById(id)) {
            transactionVoucherRepository.deleteById(id);
            cacheInvalidationService.evictTransactionVoucherCaches(id);
            return true;
        }
        return false;
    }

    // --- Custom Repository Queries ---

    public List<VoucherUsageDTO> getTopUsedVouchers(int limit) {
        return transactionVoucherRepository.findTopUsedVouchers(PageRequest.of(0, limit));
    }

    public boolean isVoucherAppliedToTransaction(Long transactionId, Long voucherId) {
        return transactionVoucherRepository.existsByTransactionIdAndVoucherId(transactionId, voucherId);
    }

    public List<TransactionVoucher> getVouchersByTransactionId(Long transactionId) {
        return transactionVoucherRepository.findByTransactionId(transactionId);
    }
}