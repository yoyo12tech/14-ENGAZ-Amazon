package com.team14.amazon.billing.strategy;

import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.billing.model.Transaction;

public interface RefundStrategy {
    RefundResult calculateRefund(Transaction transaction, RefundRequest request);
}