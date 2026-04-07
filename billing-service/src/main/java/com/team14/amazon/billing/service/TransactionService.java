package com.team14.amazon.billing.service;

import com.team14.amazon.billing.model.*;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import com.team14.amazon.billing.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionVoucherRepository transactionVoucherRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              VoucherRepository voucherRepository,
                              TransactionVoucherRepository transactionVoucherRepository) {
        this.transactionRepository = transactionRepository;
        this.voucherRepository = voucherRepository;
        this.transactionVoucherRepository = transactionVoucherRepository;
    }


    public Transaction createTransaction(Transaction transaction){
        return transactionRepository.save(transaction);
    }

    public Transaction findTransactionById(Long id){
        return transactionRepository.findById(id).orElseThrow();
    }

    public List<Transaction> findAllTransactions(){
        return transactionRepository.findAll();
    }

    public Transaction updateTransaction(Transaction transaction){
        Transaction toUpdate = transactionRepository.findById(transaction.getId()).orElseThrow();
        return transactionRepository.save(toUpdate);
    }

    public void deleteTransaction(Long id){
        transactionRepository.deleteById(id);
    }

    public Transaction findByOrderId(Long id){
        return transactionRepository.findByOrderId(id);
    }

    public Transaction findByUserId(Long id){
        return transactionRepository.findByUserId(id);
    }

    public List<Transaction> findByMethod(Method method){
        return transactionRepository.findByMethod(method);
    }

    public List<Transaction> findByStatus(Status status){
        return transactionRepository.findByStatus(status);
    }


    @Transactional
    public Transaction applyVoucherToTransaction(Long transactionId, Long voucherId) {

        // Step 1 - Find transaction by ID, throw 404 if not found
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found with id: " + transactionId));

        // Step 2 - Validate transaction status must be PENDING
        if (transaction.getStatus() != Status.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot apply voucher to a completed/cancelled transaction");
        }

        // Step 3 - Find voucher by ID, throw 404 if not found
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Voucher not found with id: " + voucherId));

        // Step 4a - Validate voucher is active
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Voucher is not active");
        }

        // Step 4b - Validate voucher expiry date is in the future
        if (voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Voucher has expired");
        }

        // Step 4c - Validate voucher usage limit has not been reached
        if (voucher.getCurrentUses() >= voucher.getMaxUses()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Voucher usage limit has been reached");
        }

        // Step 5 - Check for duplicate: voucher already applied to this transaction
        if (transactionVoucherRepository.existsByTransactionIdAndVoucherId(transactionId, voucherId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Voucher already applied");
        }

        // Step 6 - Calculate discount based on discountType
        double discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            // e.g. 500 * 20 / 100 = 100 EGP
            discount = transaction.getAmount() * voucher.getDiscountValue() / 100.0;
        } else {
            // e.g. fixed 75 EGP voucher = 75 EGP discount
            discount = voucher.getDiscountValue();
        }

        // Step 6 (cap) - Discount cannot exceed transaction amount
        if (discount > transaction.getAmount()) {
            discount = transaction.getAmount();
        }

        // Step 7 - Create the TransactionVoucher join entity
        TransactionVoucher transactionVoucher = new TransactionVoucher();
        transactionVoucher.setTransaction(transaction);
        transactionVoucher.setVoucher(voucher);
        transactionVoucher.setDiscountApplied(discount);
        transactionVoucher.setAppliedAt(LocalDateTime.now());

        // Step 8 - Increment voucher currentUses by 1
        voucher.setCurrentUses(voucher.getCurrentUses() + 1);

        // Step 9 - Update transaction amount, save all three entities
        transaction.setAmount(transaction.getAmount() - discount);
        transactionVoucherRepository.save(transactionVoucher);
        voucherRepository.save(voucher);

        // Step 10 - Return the updated transaction
        return transactionRepository.save(transaction);
    }


}
