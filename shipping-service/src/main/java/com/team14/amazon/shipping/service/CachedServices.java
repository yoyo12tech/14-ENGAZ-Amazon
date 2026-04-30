package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.dto.ShippingAnalyticsDTO;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class CachedServices {
    private final ShipmentRepository shipmentRepository;

    public CachedServices(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Cacheable(value = "shipping-service::S4-F10", key = "#start + '::' + #end")
    public ShippingAnalyticsDTO getShippingAnalyticsCached(LocalDate start, LocalDate end) {
        Object[] result = shipmentRepository.getShippingAnalytics(start, end);

        if (result == null) {
            return ShippingAnalyticsDTO.builder()
                    .totalShipments(0L)
                    .averageDeliveryDays(0.0)
                    .onTimeRate(0.0)
                    .shipmentsByStatus(Map.of())
                    .averageAttempts(0.0)
                    .build();
        }

        Map<String, Long> shipmentsByStatus = Map.of(
                "PROCESSING",       ((Number) result[3]).longValue(),
                "SHIPPED",          ((Number) result[4]).longValue(),
                "IN_TRANSIT",       ((Number) result[5]).longValue(),
                "OUT_FOR_DELIVERY", ((Number) result[6]).longValue(),
                "DELIVERED",        ((Number) result[7]).longValue(),
                "RETURNED",         ((Number) result[8]).longValue()
        );

        return ShippingAnalyticsDTO.builder()
                .totalShipments(((Number) result[0]).longValue())
                .averageDeliveryDays(((Number) result[1]).doubleValue())
                .onTimeRate(((Number) result[2]).doubleValue())
                .shipmentsByStatus(shipmentsByStatus)
                .averageAttempts(((Number) result[9]).doubleValue())
                .build();
    }
}
