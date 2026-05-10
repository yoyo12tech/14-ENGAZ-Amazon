package com.team14.amazon.shipping.repository;

import com.team14.amazon.shipping.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByOrderId(Long orderId);
//    @Query(value = "SELECT * FROM shipments WHERE CAST(status AS TEXT) = :status", nativeQuery = true)
@Query(value = "SELECT * FROM shipments WHERE status::text = :status", nativeQuery = true)
List<Shipment> findByStatusNative(@Param("status") String status);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM orders WHERE id = :orderId", nativeQuery = true)
    boolean existsOrderById(@Param("orderId") Long orderId);
    Boolean existsByOrderId(Long orderId);
    @Query("SELECT s FROM Shipment s WHERE s.orderId = :orderId ORDER BY s.lastUpdate DESC LIMIT 1")
    Optional<Shipment> findLatestShipmentByOrderId(@Param("orderId") Long orderId);
    
    // S4-F7: Purge old shipments
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM shipments WHERE last_update < :cutoff", nativeQuery = true)
    int purgeOldShipments(java.time.LocalDateTime cutoff);

    @Query(value = "SELECT * FROM shipments WHERE metadata->>:key = :value", nativeQuery = true)
    List<Shipment> findByMetadataEq(@Param("key") String key, @Param("value") String value);

    @Query(value = "SELECT * FROM shipments WHERE (metadata->>:key)::numeric > :value", nativeQuery = true)
    List<Shipment> findByMetadataGt(@Param("key") String key, @Param("value") double value);

    @Query(value = "SELECT * FROM shipments WHERE (metadata->>:key)::numeric < :value", nativeQuery = true)
    List<Shipment> findByMetadataLt(@Param("key") String key, @Param("value") double value);

    @Query(value = "SELECT * FROM shipments WHERE last_update BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp) AND (:status IS NULL OR CAST(status AS TEXT) = :status) ORDER BY last_update ASC", nativeQuery = true)
    List<Shipment> findByDateRangeAndStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status);

    // S4-F9: Delayed Shipments
    @Query(value = """
        SELECT s.id, s.order_id, s.carrier, s.tracking_number, s.estimated_delivery,
               EXTRACT(DAY FROM (CURRENT_DATE - s.estimated_delivery)) as days_overdue,
               (s.metadata->>'deliveryAttempts')::int as attempts
        FROM shipments s
        WHERE s.estimated_delivery < CURRENT_TIMESTAMP
        AND CAST(s.status AS TEXT) NOT IN ('DELIVERED', 'RETURNED')
        AND (:maxAttempts IS NULL OR (s.metadata->>'deliveryAttempts')::int <= :maxAttempts)
        """, nativeQuery = true)
    List<Object[]> findDelayedShipments(Integer maxAttempts);

     @Query(value = """
        SELECT carrier,
               COUNT(*) as totalShipments,
               COUNT(*) FILTER (WHERE status = 'DELIVERED') as deliveredCount,
               AVG(EXTRACT(EPOCH FROM (actual_delivery - created_at)) / 86400.0) FILTER (WHERE status = 'DELIVERED') as avgDays,
               (COUNT(*) FILTER (WHERE status = 'DELIVERED' AND actual_delivery <= estimated_delivery)::float /
                NULLIF(COUNT(*) FILTER (WHERE status = 'DELIVERED'), 0) * 100) as onTimeRate
        FROM shipments
        WHERE carrier = :carrier AND last_update BETWEEN :start AND :end
        GROUP BY carrier
        """, nativeQuery = true)
    List<Object[]> getCarrierSummary(String carrier, java.time.LocalDate start, java.time.LocalDate end);


     @Query(value = """
    SELECT
          COUNT(DISTINCT s.id) AS totalShipments,
          COALESCE(AVG(EXTRACT(EPOCH FROM (actual_delivery - created_at)) / 86400.0) FILTER (WHERE status = 'DELIVERED' AND actual_delivery IS NOT NULL AND actual_delivery BETWEEN :start AND :end), 0)
             AS averageDeliveryTimeDays,
          COALESCE((COUNT(DISTINCT CASE WHEN status = 'DELIVERED' AND actual_delivery <= estimated_delivery AND actual_delivery BETWEEN :start AND :end THEN s.id END)::float / NULLIF(COUNT(DISTINCT CASE WHEN status = 'DELIVERED' AND actual_delivery BETWEEN :start AND :end THEN s.id END), 0)), 0)
             AS onTimeRate,
          COUNT(DISTINCT CASE WHEN status = 'PROCESSING' AND (estimated_delivery BETWEEN :start AND :end OR actual_delivery BETWEEN :start AND :end) THEN s.id END) AS processingCount,
          COUNT(DISTINCT CASE WHEN status = 'SHIPPED' AND (estimated_delivery BETWEEN :start AND :end OR actual_delivery BETWEEN :start AND :end) THEN s.id END) AS shippedCount,
          COUNT(DISTINCT CASE WHEN status = 'IN_TRANSIT' AND (estimated_delivery BETWEEN :start AND :end OR actual_delivery BETWEEN :start AND :end) THEN s.id END) AS inTransitCount,
          COUNT(DISTINCT CASE WHEN status = 'OUT_FOR_DELIVERY' AND (estimated_delivery BETWEEN :start AND :end OR actual_delivery BETWEEN :start AND :end) THEN s.id END) AS outForDeliveryCount,
          COUNT(DISTINCT CASE WHEN status = 'DELIVERED' AND (estimated_delivery BETWEEN :start AND :end OR actual_delivery BETWEEN :start AND :end) THEN s.id END) AS deliveredCount,
          COUNT(DISTINCT CASE WHEN status = 'RETURNED' AND (estimated_delivery BETWEEN :start AND :end OR actual_delivery BETWEEN :start AND :end) THEN s.id END) AS returnedCount,
          COALESCE(AVG(COALESCE((metadata->>'deliveryAttempts')::numeric, 1)) FILTER (WHERE status = 'DELIVERED' AND actual_delivery BETWEEN :start AND :end), 0)
             AS averageAttempts
    FROM shipments s
""", nativeQuery = true)
    List<Object[]> getShippingAnalytics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

