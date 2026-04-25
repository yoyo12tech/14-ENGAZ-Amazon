package com.team14.amazon.billing.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name="vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "discount_type", nullable = false, columnDefinition = "voucher_disound_type")
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    @Column(nullable = false)
    private Integer maxUses;

    @Column(nullable = false)
    private Integer currentUses;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean active;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @JsonManagedReference("voucher-transactionvoucher")
    @OneToMany(mappedBy = "voucher")
    private List<TransactionVoucher> transactionVoucher;

    @PrePersist
    protected void onCreate() {
        this.active = true;
        this.currentUses = 0;
    }

    public Long getId() {
        return this.id;
    }

    public DiscountType getDiscountType() {
        return this.discountType;
    }

    public Double getDiscountValue() {
        return this.discountValue;
    }

    public Integer getCurrentUses() {
        return this.currentUses;
    }

    public Integer getMaxUses() {
        return this.maxUses;
    }

    public String getCode() {
        return this.code;
    }

    public Boolean getActive() {
        return this.active;
    }

    public List<TransactionVoucher> getTransactionVoucher() {
        return this.transactionVoucher;
    }

    public LocalDateTime getExpiryDate() {
        return this.expiryDate;
    }

    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public void setId(Long id){
        this.id = id;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setTransactionVoucher(List<TransactionVoucher> transactionVoucher) {
        this.transactionVoucher = transactionVoucher;
    }

    public void setCurrentUses(Integer currentUses) {
        this.currentUses = currentUses;
    }
}

