package com.team14.amazon.shipping.repository.cassandra;
import com.team14.amazon.shipping.model.cassandra.ShipmentTrackingEvent;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShipmentTrackingEventRepository extends CassandraRepository<ShipmentTrackingEvent, Long> {

    /**
     * Retrieves all tracking events for a specific shipment.
     * Because 'timestamp' is a descending clustering key, Cassandra will
     * automatically return this list sorted newest-first!
     *
     * @param shipmentId The partition key
     * @return List of events sorted by timestamp DESC
     */

    @Query("SELECT * FROM shipment_tracking_events WHERE shipment_id = ?0 AND \"timestamp\" >= ?1 AND \"timestamp\" <= ?2")
    List<ShipmentTrackingEvent> findByShipmentIdAndTimeRange(Long shipmentId, Instant start, Instant end);

    @Query("SELECT * FROM shipment_tracking_events WHERE shipment_id = ?0")
    List<ShipmentTrackingEvent> findByShipmentId(Long shipmentId);

}