package com.team14.amazon.billing.service;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import com.team14.amazon.billing.dto.RetryTransactionResponse;
import java.util.NoSuchElementException;

import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
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
    public RetryTransactionResponse retryTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != Status.FAILED) {
            throw new IllegalStateException("Only FAILED transactions can be retried. Current status: " + transaction.getStatus());
        }

        Status previous = transaction.getStatus();
        transaction.setStatus(Status.PENDING);
        transactionRepository.save(transaction);

        return new RetryTransactionResponse(
            transaction.getId(), previous, Status.PENDING,
            "Transaction queued for retry successfully"
        );
    }
}
