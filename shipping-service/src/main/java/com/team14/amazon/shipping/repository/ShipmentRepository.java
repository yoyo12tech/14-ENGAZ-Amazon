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
    // S4-F7: Purge old shipments
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM shipments WHERE lastUpdate < :cutoff", nativeQuery = true)
    int purgeOldShipments(java.time.LocalDateTime cutoff);

    // S4-F8: Carrier Summary
    @Query(value = """
        SELECT carrier,
               COUNT(*) as totalShipments,
               COUNT(*) FILTER (WHERE status = 'DELIVERED') as deliveredCount,
               AVG(EXTRACT(DAY FROM (actual_delivery - created_at))) FILTER (WHERE status = 'DELIVERED') as avgDays,
               (COUNT(*) FILTER (WHERE status = 'DELIVERED' AND actual_delivery <= estimated_delivery)::float /
                NULLIF(COUNT(*) FILTER (WHERE status = 'DELIVERED'), 0) * 100) as onTimeRate
        FROM shipments
        WHERE carrier = :carrier AND last_update BETWEEN :start AND :end
        GROUP BY carrier
        """, nativeQuery = true)
    List<Object[]> getCarrierSummary(String carrier, java.time.LocalDate start, java.time.LocalDate end);
}

