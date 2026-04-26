package com.team14.amazon.shipping.adapter;

import com.team14.amazon.shipping.dto.CarrierSummaryDTO;
import com.team14.amazon.shipping.dto.DelayedShipmentDTO;

import java.time.LocalDateTime;

public class ObjectArrayDtoAdapter {

    public static DelayedShipmentDTO adaptToDelayedShipmentDTO(Object[] row) {
        return DelayedShipmentDTO.builder()
                .shipmentId(((Number) row[0]).longValue())
                .orderId(((Number) row[1]).longValue())
                .carrier((String) row[2])
                .trackingNumber((String) row[3])
                .estimatedDelivery((LocalDateTime) row[4])
                .daysOverdue(((Number) row[5]).longValue())
                .deliveryAttempts((Integer) row[6])
                .build();
    }

    public static CarrierSummaryDTO adaptToCarrierSummaryDTO(Object[] row, String carrier) {
        return CarrierSummaryDTO.builder()
                .carrier(row[0] != null ? row[0].toString() : carrier)
                .totalShipments(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                .deliveredCount(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                .averageDeliveryDays(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .onTimeRate(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                .build();
    }
}