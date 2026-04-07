package com.team14.amazon.billing.dto;

import com.team14.amazon.billing.model.Method;
import com.team14.amazon.billing.model.Status;

import java.util.List;
import java.util.Map;

public record TransactionDetailsDTO(Long transactionId,
                                    Long orderId,
                                    Long userId,
                                    Double originalAmount,
                                    Method method,
                                    Status status,
                                    Map<String, Object> transactionDetails,
                                    List<Map<String, Object>> appliedVouchers,
                                    Double totalDiscount,
                                    Double finalAmount) {

}
