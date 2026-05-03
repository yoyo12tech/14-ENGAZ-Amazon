package com.team14.amazon.shipping.model.cassandra;
import com.team14.amazon.shipping.model.Shipment;
import lombok.*;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("shipment_tracking_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentTrackingEvent {

    @PrimaryKeyColumn(name = "shipment_id", type = PrimaryKeyType.PARTITIONED)
    private Long shipmentId;

    // CLUSTERING KEY: Sorts events newest-first within the partition
    @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant timestamp;

    @Column("status")
    private Shipment.Status status;

    @Column("carrier")
    private String carrier;

    @Column("tracking_number")
    private String trackingNumber;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    @Column("notes")
    private String notes;
}

