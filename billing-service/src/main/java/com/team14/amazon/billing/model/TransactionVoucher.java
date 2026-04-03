package com.team14.amazon.billing.model;

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
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
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

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public void setDiscountApplied(Double discountApplied) {
        this.discountApplied = discountApplied;
    }
}
