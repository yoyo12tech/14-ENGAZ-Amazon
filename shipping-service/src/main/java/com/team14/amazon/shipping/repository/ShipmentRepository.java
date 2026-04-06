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

    @Query(value = "SELECT * FROM shipments WHERE metadata->>:key = :value", nativeQuery = true)
    List<Shipment> findByMetadataEq(@Param("key") String key, @Param("value") String value);

    @Query(value = "SELECT * FROM shipments WHERE (metadata->>:key)::numeric > :value", nativeQuery = true)
    List<Shipment> findByMetadataGt(@Param("key") String key, @Param("value") double value);

    @Query(value = "SELECT * FROM shipments WHERE (metadata->>:key)::numeric < :value", nativeQuery = true)
    List<Shipment> findByMetadataLt(@Param("key") String key, @Param("value") double value);
}


