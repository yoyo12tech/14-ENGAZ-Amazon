package com.team14.amazon.billing.service;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.dto.RefundResponse;
import com.team14.amazon.billing.model.Refund;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.repository.RefundRepository;
import com.team14.amazon.billing.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class RefundService {
    private final RefundRepository refundRepository;
    private final TransactionRepository transactionRepository;

    public RefundService(RefundRepository refundRepository,
                         TransactionRepository transactionRepository) {
        this.refundRepository = refundRepository;
        this.transactionRepository = transactionRepository;
    }

    public RefundResponse processRefund(RefundRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + request.getTransactionId()));

        if (transaction.getStatus() != Status.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED transactions are eligible for a refund. Current status: " + transaction.getStatus());
        }

        Refund refund = new Refund();
        refund.setTransaction(transaction);
        refund.setReason(request.getReason());
        refund.setAmount(transaction.getAmount());
        refund.setMetadata(request.getMetadata());

        Refund saved = refundRepository.save(refund);

        transaction.setStatus(Status.REFUNDED);
        transactionRepository.save(transaction);

        return new RefundResponse(
                saved.getId(),
                transaction.getId(),
                saved.getAmount(),
                saved.getReason(),
                saved.getMetadata(),
                saved.getCreatedAt()
        );
    }
}