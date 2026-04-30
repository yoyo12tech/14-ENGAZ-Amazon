package com.team14.amazon.shipping.repository.cassandra;
import com.team14.amazon.shipping.model.cassandra.ShipmentTrackingEvent;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

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
    List<ShipmentTrackingEvent> findByShipmentId(Long shipmentId);

}