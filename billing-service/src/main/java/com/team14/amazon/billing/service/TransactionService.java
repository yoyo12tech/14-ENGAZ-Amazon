package com.team14.amazon.billing.service;

import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.dto.RevenueReportDTO;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.dto.UserTransactionSummaryDTO;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import com.team14.amazon.billing.repository.VoucherRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.billing.dto.ProcessTransactionRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionVoucherRepository transactionVoucherRepository;

    public TransactionService(TransactionRepository transactionRepository, VoucherRepository voucherRepository, TransactionVoucherRepository transactionVoucherRepository) {
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

    public RevenueReportDTO getRevenueReport(String startDateStr, String endDateStr) {
        LocalDateTime startDate;
        LocalDateTime endDate;
        try {
            startDate = LocalDate.parse(startDateStr).atStartOfDay();
            endDate = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format", e);
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date");
        }

        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(startDate, endDate);

        double totalRevenue = 0.0;
        long totalTransactions = 0;
        double refundedAmount = 0.0;
        long refundCount = 0;

        for (Transaction t : transactions) {
            if (t.getStatus() == Status.COMPLETED) {
                totalRevenue += t.getAmount();
                totalTransactions++;
            } else if (t.getStatus() == Status.REFUNDED) {
                refundedAmount += t.getAmount();
                refundCount++;
            }
        }

        double averageTransaction = 0.0;
        if (totalTransactions > 0) {
            averageTransaction = totalRevenue / totalTransactions;
        }

        return new RevenueReportDTO(totalRevenue, totalTransactions, averageTransaction, refundedAmount, refundCount);
    }

    public UserTransactionSummaryDTO getUserTransactionSummary(Long userId) {
        if (!transactionRepository.checkUserExists(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Object[]> results = transactionRepository.getCompletedTransactionSummaryByUser(userId, Status.COMPLETED);

        long totalTransactions = 0;
        double totalAmount = 0.0;
        Map<String, Double> methodBreakdown = new HashMap<>();

        for (Object[] row : results) {
            String methodVal = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count = ((Number) row[1]).longValue();
            double sum = ((Number) row[2]).doubleValue();

            totalTransactions += count;
            totalAmount += sum;
            methodBreakdown.put(methodVal, sum);
        }

        return new UserTransactionSummaryDTO(userId, totalTransactions, totalAmount, methodBreakdown);
    }

    public List<Transaction> searchTransactions(String statusStr, String startDateStr, String endDateStr) {
        Status status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = Status.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid status value"
                );
            }
        }

        LocalDateTime startDate = null;
        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            try {
                startDate = java.time.LocalDate.parse(startDateStr).atStartOfDay();
            } catch (java.time.format.DateTimeParseException e) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid startDate format. Use YYYY-MM-DD."
                );
            }
        }

        LocalDateTime endDate = null;
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                endDate = java.time.LocalDate.parse(endDateStr).atTime(java.time.LocalTime.MAX);
            } catch (java.time.format.DateTimeParseException e) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid endDate format. Use YYYY-MM-DD."
                );
            }
        }

        return transactionRepository.findTransactionsByFilters(status, startDate, endDate);
    }
    public Transaction processOrderTransaction(Long orderId, ProcessTransactionRequest request) {
        Transaction existing = transactionRepository.findByOrderId(orderId);
        if (existing != null && existing.getStatus() == Status.COMPLETED) {
            throw new IllegalStateException("already paid");
        }
        if (existing == null) {
            throw new NoSuchElementException("No transaction found for order: " + orderId);
        }

        existing.setMethod(request.getMethod());
        existing.setStatus(Status.COMPLETED);

        Map<String, Object> details = new HashMap<>();
        if (existing.getTransactionDetails() != null) {
             details.putAll(existing.getTransactionDetails());
        }
        details.put("gatewayResponse", "approved");
        if (request.getCardLastFour() != null) {
            details.put("cardLastFour", request.getCardLastFour());
        }
        existing.setTransactionDetails(details);

        return transactionRepository.save(existing);
}
    public Transaction processRefund(Long transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != Status.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED transactions are eligible for a refund. Current status: " + transaction.getStatus());
        }

        transaction.setStatus(Status.REFUNDED);

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        details.put("refundReason", reason);
        details.put("refundedAt", LocalDateTime.now().toString());
        transaction.setTransactionDetails(details);

        return transactionRepository.save(transaction);
    }

    public List<VoucherUsageDTO> getTopUsedVouchers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionVoucherRepository.findTopUsedVouchers((Pageable) pageable);
}
    public Transaction retryTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != Status.FAILED) {
            throw new IllegalStateException("Only FAILED transactions can be retried. Current status: " + transaction.getStatus());
        }

        transaction.setStatus(Status.COMPLETED);

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        int retryAttempt = 0;
        if (details.get("retryAttempt") instanceof Number) {
            retryAttempt = ((Number) details.get("retryAttempt")).intValue();
        }
        details.put("retryAttempt", retryAttempt + 1);
        details.put("gatewayResponse", "approved");
        transaction.setTransactionDetails(details);

        return transactionRepository.save(transaction);
    }
}
