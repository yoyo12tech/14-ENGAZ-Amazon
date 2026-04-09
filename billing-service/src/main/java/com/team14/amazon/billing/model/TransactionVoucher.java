package com.team14.amazon.billing.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_vouchers")
public class TransactionVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="discount_applied", nullable = false)
    private Double discountApplied;

    @Column(name="applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonBackReference("transaction")
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    @JsonBackReference("voucher")
    private Voucher voucher;

    public Long getId() {
        return this.id;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public Voucher getVoucher() {
        return this.voucher;
    }

    public Double getDiscountApplied() {
        return this.discountApplied;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public void setDiscountApplied(Double discountApplied) {
        this.discountApplied = discountApplied;
    }
    public void setAppliedAt(LocalDateTime appliedAt){ this.appliedAt = appliedAt;}
}
