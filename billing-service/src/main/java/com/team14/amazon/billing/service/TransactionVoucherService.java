package com.team14.amazon.billing.service;

import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.model.TransactionVoucher;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionVoucherService {

    private final TransactionVoucherRepository transactionVoucherRepository;

    public TransactionVoucherService(TransactionVoucherRepository transactionVoucherRepository) {
        this.transactionVoucherRepository = transactionVoucherRepository;
    }

    // --- Basic CRUD Operations ---

    public TransactionVoucher create(TransactionVoucher transactionVoucher) {
        return transactionVoucherRepository.save(transactionVoucher);
    }

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
            return transactionVoucherRepository.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (transactionVoucherRepository.existsById(id)) {
            transactionVoucherRepository.deleteById(id);
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