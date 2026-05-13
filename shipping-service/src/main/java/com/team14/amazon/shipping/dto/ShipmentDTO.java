package com.team14.amazon.shipping.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDTO {
    private Long id;
    private String status;
    private String trackingNumber;
    private LocalDateTime lastUpdate;
}
