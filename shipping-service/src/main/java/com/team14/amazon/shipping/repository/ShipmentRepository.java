package com.team14.amazon.shipping.repository;

import com.team14.amazon.shipping.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByOrderId(Long orderId);
    Boolean existsByOrderId(Long orderId);
    @Query("SELECT s FROM Shipment s WHERE s.orderId = :orderId ORDER BY s.lastUpdate DESC LIMIT 1")
    Optional<Shipment> findLatestShipmentByOrderId(@Param("orderId") Long orderId);
}
