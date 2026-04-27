package com.team14.amazon.billing.adapter;

import com.team14.amazon.billing.dto.UserTransactionSummaryDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectArrayDtoAdapter {

    public UserTransactionSummaryDTO adapt(Long userId, List<Object[]> queryResults) {
        long totalTransactions = 0;
        double totalAmount = 0.0;
        Map<String, Double> methodBreakdown = new HashMap<>();

        for (Object[] row : queryResults) {
            String methodVal = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count = ((Number) row[1]).longValue();
            double sum = ((Number) row[2]).doubleValue();

            totalTransactions += count;
            totalAmount += sum;
            methodBreakdown.put(methodVal, sum);
        }

        return UserTransactionSummaryDTO.builder()
                .userId(userId)
                .totalTransactions(totalTransactions)
                .totalAmount(totalAmount)
                .methodBreakdown(methodBreakdown)
                .build();
    }
}

