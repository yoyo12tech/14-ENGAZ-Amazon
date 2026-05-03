package com.team14.amazon.shipping.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentTrackingDTO {
    private LocalDateTime timestamp;
    private String status;
    private String carrier;
    private String trackingNumber;
    private Double latitude;
    private Double longitude;
    private String notes;
}