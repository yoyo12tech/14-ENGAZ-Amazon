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
import java.util.Set;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findFirstByOrderIdAndStatusIn(Long orderId, Set<Shipment.Status> statuses);

    // @Query(value = "SELECT * FROM shipments WHERE CAST(status AS TEXT) =
    // :status", nativeQuery = true)
    @Query(value = "SELECT * FROM shipments WHERE status::text = :status", nativeQuery = true)
    List<Shipment> findByStatusNative(@Param("status") String status);

//    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM orders WHERE id = :orderId", nativeQuery = true)
//    boolean existsOrderById(@Param("orderId") Long orderId);

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
                   COALESCE(AVG(actual_delivery::date - created_at::date) FILTER (WHERE status = 'DELIVERED'), 0) as avgDays,
                   COALESCE(CAST(COUNT(*) FILTER (WHERE status = 'DELIVERED' AND actual_delivery::date <= estimated_delivery::date) AS float) /
                    NULLIF(COUNT(*) FILTER (WHERE status = 'DELIVERED'), 0), 0) as onTimeRate
            FROM shipments
            WHERE carrier = :carrier AND created_at >= CAST(:start AS timestamp) AND created_at < CAST(:end AS timestamp) + INTERVAL '1 day'
            GROUP BY carrier
            """, nativeQuery = true)
    List<Object[]> getCarrierSummary(String carrier, java.time.LocalDate start, java.time.LocalDate end);

    @Query(value = """
               SELECT
                     COUNT(*) AS totalShipments,
                     COALESCE(AVG(actual_delivery::date - created_at::date) FILTER (WHERE status = 'DELIVERED' AND actual_delivery IS NOT NULL), 0)
                        AS averageDeliveryTimeDays,
                     COALESCE(CAST(COUNT(*) FILTER (WHERE status = 'DELIVERED' AND actual_delivery::date <= estimated_delivery::date) AS float) / NULLIF(COUNT(*) FILTER (WHERE status = 'DELIVERED'), 0), 0)
                        AS onTimeRate,
                     COUNT(*) FILTER (WHERE status = 'PROCESSING') AS processingCount,
                     COUNT(*) FILTER (WHERE status = 'SHIPPED')                      AS shippedCount,
                     COUNT(*) FILTER (WHERE status = 'IN_TRANSIT')                   AS inTransitCount,
                     COUNT(*) FILTER (WHERE status = 'OUT_FOR_DELIVERY')             AS outForDeliveryCount,
                     COUNT(*) FILTER (WHERE status = 'DELIVERED')                    AS deliveredCount,
                     COUNT(*) FILTER (WHERE status = 'RETURNED')                     AS returnedCount,
                     COALESCE(AVG(COALESCE((metadata->>'deliveryAttempts')::numeric, 1)) FILTER (WHERE status = 'DELIVERED'), 0)
                        AS averageAttempts
               FROM shipments
               WHERE created_at >= CAST(:start AS timestamp) AND created_at <= CAST(:end AS timestamp)
            """, nativeQuery = true)
    List<Object[]> getShippingAnalytics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(
            value = """
            SELECT * FROM shipments s
            WHERE s.order_id = :orderId
            AND s.status IN ('PROCESSING','SHIPPED','IN_TRANSIT','OUT_FOR_DELIVERY')
            ORDER BY s.last_update DESC
            LIMIT 1
        """, nativeQuery = true
    )
    Shipment getActiveShipment(@Param("orderId") Long orderId);

    @Query(
            value = """
            SELECT s FROM Shipment s
            WHERE s.orderId = :orderId
            AND s.status IN ('PROCESSING','SHIPPED','IN_TRANSIT','OUT_FOR_DELIVERY')
            ORDER BY s.lastUpdate DESC
            LIMIT 1
        """
    )
    Optional<Shipment> findActiveShipmentByOrderId(@Param("orderId") Long orderId);

    // Returns all shipment IDs for a given orderId — used by /ids endpoint (S5-F11)
    @Query(
            value = "SELECT s.id FROM shipments s WHERE s.order_id = :orderId",
            nativeQuery = true
    )
    List<Long> findShipmentIdsByOrderId(@Param("orderId") Long orderId);
}
