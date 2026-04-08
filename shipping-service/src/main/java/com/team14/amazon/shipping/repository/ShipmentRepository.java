package com.team14.amazon.shipping.repository;

import com.team14.amazon.shipping.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByOrderId(Long orderId);

    // S4-F9: Delayed Shipments
    @Query(value = """
        SELECT s.id, s.order_id, s.carrier, s.tracking_number, s.estimated_delivery,
               EXTRACT(DAY FROM (CURRENT_DATE - s.estimated_delivery)) as days_overdue,
               (s.metadata->>'deliveryAttempts')::int as attempts
        FROM shipments s
        WHERE s.estimated_delivery < CURRENT_TIMESTAMP
        AND s.status NOT IN ('DELIVERED', 'RETURNED')
        AND (:maxAttempts IS NULL OR (s.metadata->>'deliveryAttempts')::int <= :maxAttempts)
        """, nativeQuery = true)
    List<Object[]> findDelayedShipments(Integer maxAttempts);
}

