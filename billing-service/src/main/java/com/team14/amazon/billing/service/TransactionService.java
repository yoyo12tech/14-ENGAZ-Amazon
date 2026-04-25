package com.team14.amazon.billing.service;

import com.team14.amazon.billing.dto.VoucherUsageDTO;
import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.*;
import com.team14.amazon.billing.dto.RevenueReportDTO;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.dto.UserTransactionSummaryDTO;
import com.team14.amazon.billing.repository.TransactionVoucherRepository;
import com.team14.amazon.billing.repository.VoucherRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.team14.amazon.billing.dto.AppliedVoucherDetail;
import com.team14.amazon.billing.dto.TransactionDetailsDTO;

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
        return transactionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    }

    public List<Transaction> findAllTransactions(){
        return transactionRepository.findAll();
    }

    public Transaction updateTransaction(Transaction transaction) {
        // Check if it exists to throw an error if missing, then save the NEW data
        if (!transactionRepository.existsById(transaction.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        return transactionRepository.save(transaction);
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

    public List<Transaction> findByMethod(TransactionMethod method){
        return transactionRepository.findByMethod(method);
    }

    public List<Transaction> findByStatus(TransactionStatus status){
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
            if (t.getStatus() == TransactionStatus.COMPLETED) {
                totalRevenue += t.getAmount();
                totalTransactions++;
            } else if (t.getStatus() == TransactionStatus.REFUNDED) {
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
        if (!transactionRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Object[]> results = transactionRepository.getCompletedTransactionSummaryByUser(userId, TransactionStatus.COMPLETED);

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
        List<Transaction> transactions = transactionRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        return transactions.stream().filter(t -> {
            boolean matches = true;
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                matches = matches && t.getStatus().name().equalsIgnoreCase(statusStr);
            }
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                LocalDateTime start;
                try {
                    // Try parsing as ISO Date (2026-04-10)
                    start = LocalDate.parse(startDateStr).atStartOfDay();
                } catch (Exception e) {
                    // Fallback for full DateTime strings
                    start = LocalDateTime.parse(startDateStr);
                }
                matches = matches && !t.getCreatedAt().isBefore(start);
            }
            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                java.time.LocalDateTime end;
                try {
                    end = java.time.LocalDate.parse(endDateStr).atTime(java.time.LocalTime.MAX);
                } catch (Exception e) {
                    end = java.time.LocalDateTime.parse(endDateStr); // Fallback for full timestamps
                }
                matches = matches && !t.getCreatedAt().isAfter(end);
            }
            return matches;
        }).toList();
    }
    @Transactional
    public Transaction processOrderTransaction(Long orderId, ProcessTransactionRequest request) {
        // 1. Verify order exists → 404 if not
        String orderStatus = transactionRepository.findOrderStatusByOrderId(orderId);
        if (orderStatus == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Order not found: " + orderId);
        }

        // 2. Verify order is DELIVERED → 400 if not
        if (!orderStatus.equalsIgnoreCase("DELIVERED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DELIVERED orders can be processed");
        }

        // 3. Find the transaction created at delivery time
        Transaction existing = transactionRepository.findByOrderId(orderId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No transaction found for order: " + orderId);
        }

        // 4. Check already paid → 400
        if (existing.getStatus() == TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        // 5. Process
        existing.setMethod(request.getMethod());
        existing.setStatus(TransactionStatus.COMPLETED);

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only COMPLETED transactions are eligible for a refund.");
        }

        transaction.setStatus(TransactionStatus.REFUNDED);

        Map<String, Object> details = new HashMap<>();
        if (transaction.getTransactionDetails() != null) {
            details.putAll(transaction.getTransactionDetails());
        }
        details.put("refundReason", reason);
        details.put("refundedAt", LocalDateTime.now().toString());
        transaction.setTransactionDetails(details);

        return transactionRepository.save(transaction);
    }
    @Transactional
    public Transaction applyVoucher(Long transactionId, Long voucherId) {
        //Find transaction
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction with id: " + transactionId + " was not found"));

        //transaction status
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cannot apply voucher to a completed/cancelled transaction");
        }
        //Find voucher
        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Voucher with id: " + voucherId + " was not found"));

        int currentUses = voucher.getCurrentUses() != null ? voucher.getCurrentUses() : 0;
        int maxUses = voucher.getMaxUses() != null ? voucher.getMaxUses() : Integer.MAX_VALUE;
        double txAmount = transaction.getAmount() != null ? transaction.getAmount() : 0.0;
        double discValue = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : 0.0;
        //check voucher usability
        if (!voucher.getActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is inactive");
        }
        if (voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher has expired");
        }
        if (voucher.getCurrentUses() >= voucher.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher usage limit reached");
        }

        //check for dups
        if (transactionVoucherRepository.existsByTransactionIdAndVoucherId(transactionId, voucherId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "voucher already applied");
        }

        //calculate discount
        double discount;
        discount = (voucher.getDiscountType() == DiscountType.PERCENTAGE) ? (txAmount * discValue / 100) : discValue;
        discount = Math.min(discount, txAmount);

        //create join entity
        TransactionVoucher transactionVoucher = new TransactionVoucher();
        transactionVoucher.setTransaction(transaction);
        transactionVoucher.setVoucher(voucher);
        transactionVoucher.setDiscountApplied(discount);
        transactionVoucher.setAppliedAt(LocalDateTime.now());


            //increment use
        voucher.setCurrentUses(voucher.getCurrentUses() + 1);

            //save
        voucherRepository.save(voucher);
        transactionVoucherRepository.save(transactionVoucher);
        //refresh transaction's voucher list
        transaction.getTransactionVouchers().add(transactionVoucher);
        transactionRepository.save(transaction);

        return transaction;
        }
    public Transaction retryTransaction (Long transactionId){
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        if (transaction.getStatus() != TransactionStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only FAILED transactions can be retried"); // Was incorrectly set to NOT_FOUND
        }

        transaction.setStatus(TransactionStatus.COMPLETED);

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
        public TransactionDetailsDTO getTransactionDetails (Long transactionId){
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

            List<AppliedVoucherDetail> appliedVouchers = transaction.getTransactionVouchers() == null
                    ? List.of()
                    : transaction.getTransactionVouchers().stream()
                    .map(tv -> new AppliedVoucherDetail(
                            tv.getVoucher().getCode(),
                            tv.getVoucher().getDiscountType(),
                            tv.getDiscountApplied(),
                            tv.getAppliedAt()
                    )).toList();

            double totalDiscount = appliedVouchers.stream()
                    .mapToDouble(AppliedVoucherDetail::getDiscountApplied)
                    .sum();

            double finalAmount = transaction.getAmount() - totalDiscount;

            return new TransactionDetailsDTO(
                    transaction.getId(),
                    transaction.getOrderId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getMethod(),
                    transaction.getStatus(),
                    transaction.getCreatedAt(),
                    transaction.getTransactionDetails(),
                    appliedVouchers,
                    totalDiscount,
                    finalAmount
            );
        }
    public List<VoucherUsageDTO> getTopUsedVouchers ( int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionVoucherRepository.findTopUsedVouchers((Pageable) pageable);
    }
}


