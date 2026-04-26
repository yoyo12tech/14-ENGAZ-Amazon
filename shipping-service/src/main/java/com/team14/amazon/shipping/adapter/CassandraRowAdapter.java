package com.team14.amazon.shipping.adapter;

import com.team14.amazon.shipping.dto.NearbyShipmentDTO;
import com.datastax.oss.driver.api.core.cql.Row;

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
}