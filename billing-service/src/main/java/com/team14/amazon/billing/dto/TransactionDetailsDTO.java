package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TransactionDetailsDTO {
    private Long transactionId;
    private Long orderId;
    private Long userId;
    private Double originalAmount;
    private TransactionMethod method;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private Map<String, Object> transactionDetails;
    private List<AppliedVoucherDetail> appliedVouchers;
    private Double totalDiscount;
    private Double finalAmount;

    public TransactionDetailsDTO(Long transactionId, Long orderId, Long userId,
                                  Double originalAmount, TransactionMethod method, TransactionStatus status,
                                  LocalDateTime createdAt, Map<String, Object> transactionDetails,
                                  List<AppliedVoucherDetail> appliedVouchers,
                                  Double totalDiscount, Double finalAmount) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.userId = userId;
        this.originalAmount = originalAmount;
        this.method = method;
        this.status = status;
        this.createdAt = createdAt;
        this.transactionDetails = transactionDetails;
        this.appliedVouchers = appliedVouchers;
        this.totalDiscount = totalDiscount;
        this.finalAmount = finalAmount;
    }

    public Long getTransactionId() { return transactionId; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Double getOriginalAmount() { return originalAmount; }
    public TransactionMethod getMethod() { return method; }
    public TransactionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Map<String, Object> getTransactionDetails() { return transactionDetails; }
    public List<AppliedVoucherDetail> getAppliedVouchers() { return appliedVouchers; }
    public Double getTotalDiscount() { return totalDiscount; }
    public Double getFinalAmount() { return finalAmount; }
}