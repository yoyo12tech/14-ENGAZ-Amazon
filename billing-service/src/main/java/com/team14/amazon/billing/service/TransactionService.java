package com.team14.amazon.billing.service;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import com.team14.amazon.billing.dto.OrderTransactionRequest;
import com.team14.amazon.billing.dto.OrderTransactionResponse;

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
    public OrderTransactionResponse processOrderTransaction(OrderTransactionRequest request) {
        Transaction existing = transactionRepository.findByOrderId(request.getOrderId());
        if (existing != null) {
            throw new IllegalStateException("Transaction already exists for order: " + request.getOrderId());
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(request.getOrderId());
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setMethod(request.getMethod());
        transaction.setStatus(Status.PENDING);
        Transaction saved = transactionRepository.save(transaction);

        return new OrderTransactionResponse(
            saved.getId(), saved.getOrderId(), saved.getUserId(),
            saved.getAmount(), saved.getMethod(), saved.getStatus(), saved.getCreatedAt()
        );
    }
}
