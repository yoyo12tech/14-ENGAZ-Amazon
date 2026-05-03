package com.team14.amazon.shipping.adapter;

import com.team14.amazon.shipping.dto.NearbyShipmentDTO;
import com.datastax.oss.driver.api.core.cql.Row;
import com.team14.amazon.shipping.dto.ShipmentTrackingDTO;
import com.team14.amazon.shipping.model.cassandra.ShipmentTrackingEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class CassandraRowAdapter {

    public NearbyShipmentDTO adapt(Row row) {
        return NearbyShipmentDTO.builder()
                .shipmentId(row.getLong("shipment_id"))
                .orderId(row.getLong("order_id"))
                .carrier(row.getString("carrier"))
                .trackingNumber(row.getString("tracking_number"))
                .latitude(row.getDouble("latitude"))
                .longitude(row.getDouble("longitude"))
                .distanceKm(row.getDouble("distance_km"))
                .build();
    }

    public ShipmentTrackingDTO adapt(ShipmentTrackingEvent event) {
        // Using the Builder created in the DTO class
        return ShipmentTrackingDTO.builder()
                .timestamp(event.getTimestamp()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .status(String.valueOf(event.getStatus()))
                .carrier(event.getCarrier())
                .trackingNumber(event.getTrackingNumber())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .notes(event.getNotes())
                .build();
    }
}