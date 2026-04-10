package com.team14.amazon.billing.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name="transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "method", nullable = false, columnDefinition = "transaction_method")
    private Method method;

    
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> transactionDetails;

    @Column(name= "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<TransactionVoucher> transactionVouchers;

    @PrePersist
    protected void OnCreate(){
        this.createdAt = LocalDateTime.now();
    }


    public Long getId() {
        return this.id;
    }

    public Long getOrderId() {
        return this.orderId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public Double getAmount() {
        return this.amount;
    }

    public Method getMethod() {
        return this.method;
    }

    public Status getStatus(){
        return this.status;
    }

    public Map<String, Object> getTransactionDetails() {
        return this.transactionDetails;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public List<TransactionVoucher> getTransactionVouchers() {
        return this.transactionVouchers;
    }

    public void setId(Long id){
        this.id = id;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTransactionDetails(Map<String, Object> transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    public void setTransactionVouchers(List<TransactionVoucher> transactionVouchers) {
        this.transactionVouchers = transactionVouchers;
    }
}


