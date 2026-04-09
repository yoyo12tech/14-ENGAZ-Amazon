package com.team14.amazon.shipping.repository;

import com.team14.amazon.shipping.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByOrderId(Long orderId);
    List<Shipment> findByStatus(Shipment.Status status);
//    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM orders WHERE id = :orderId")
    boolean existsOrderById(@Param("orderId") Long orderId);
    Boolean existsByOrderId(Long orderId);
    @Query("SELECT s FROM Shipment s WHERE s.orderId = :orderId ORDER BY s.lastUpdate DESC LIMIT 1")
    Optional<Shipment> findLatestShipmentByOrderId(@Param("orderId") Long orderId);
    // S4-F7: Purge old shipments
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM shipments WHERE last_update < :cutoff", nativeQuery = true)
    int purgeOldShipments(java.time.LocalDateTime cutoff);

    @Query(value = "SELECT * FROM shipments WHERE last_update BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp) AND (:status IS NULL OR status = :status) ORDER BY last_update ASC", nativeQuery = true)
    List<Shipment> findByDateRangeAndStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status);
}

