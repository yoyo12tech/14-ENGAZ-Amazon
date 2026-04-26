package com.team14.amazon.shipping.adapter;

import com.team14.amazon.shipping.dto.NearbyShipmentDTO;
import org.bson.Document;

public class MongoDocumentAdapter {

    public NearbyShipmentDTO adapt(Document document) {
        return NearbyShipmentDTO.builder()
                .shipmentId(document.getLong("shipmentId"))
                .orderId(document.getLong("orderId"))
                .carrier(document.getString("carrier"))
                .trackingNumber(document.getString("trackingNumber"))
                .latitude(document.getDouble("latitude"))
                .longitude(document.getDouble("longitude"))
                .distanceKm(document.getDouble("distanceKm"))
                .build();
    }
}