package com.team14.amazon.shipping.controller;

import com.team14.amazon.contracts.dto.ShipmentDTO;
import com.team14.amazon.shipping.dto.*;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.model.cassandra.ShipmentTrackingEvent;
import com.team14.amazon.shipping.model.mongo.ShipmentEvent;
import com.team14.amazon.shipping.service.ShipmentService;
import org.antlr.v4.runtime.misc.LogManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.shipping.dto.ShipmentTrackingRequestDTO;
import com.team14.amazon.shipping.model.cassandra.ShipmentTrackingEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }


    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    // S4-F8: Carrier Performance Summary
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/carrier/{carrier}/summary")
    public ResponseEntity<CarrierSummaryDTO> getCarrierSummary(
            @PathVariable String carrier,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate) {
        return ResponseEntity.ok(shipmentService.getCarrierSummary(carrier, startDate, endDate));
    }

    // S4-F9: Find Delayed Shipments
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/delayed")
    public ResponseEntity<List<DelayedShipmentDTO>> getDelayedShipments(@RequestParam(required = false) Integer maxDeliveryAttempts) {
        return ResponseEntity.ok(shipmentService.getDelayedShipments(maxDeliveryAttempts));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable Long id) {
        return shipmentService.getShipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createShipment(@RequestBody Shipment shipment) {
        try {
            Shipment createdShipment = shipmentService.createShipment(shipment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdShipment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/purge")
    public ResponseEntity<Integer> purgeOldShipments(@RequestParam int olderThanDays) {
        return ResponseEntity.ok(shipmentService.purgeShipments(olderThanDays));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Shipment> updateShipment(@PathVariable Long id, @RequestBody Shipment updatedShipment) {
        return shipmentService.updateShipment(id, updatedShipment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(@PathVariable Long id) {
        if (shipmentService.deleteShipment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyShipmentDTO>> findNearbyShipments(@RequestParam Double lat, @RequestParam Double lon, @RequestParam Double radiusKm){
        return ResponseEntity.ok(shipmentService.findNearbyShipments(lat, lon, radiusKm));
}
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/order/{orderId}")
    public ResponseEntity<?> createShipmentForOrder(@PathVariable Long orderId, @RequestBody Shipment shipment){
        try{
            Shipment created = shipmentService.createShipment(orderId, shipment);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("order/{orderId}/latest")
    public Shipment getLatestShipmentForAnOrder(@PathVariable Long orderId){
        return shipmentService.getLatestShipmentForOrder(orderId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/batch-status")
    public ResponseEntity<?> batchUpdateStatus(@RequestBody List<BatchStatusUpdateRequest> updates) {
        if (updates == null || updates.isEmpty()) {
            return ResponseEntity.ok(Map.of("updatedCount", 0));
        }
        try {
            int count = shipmentService.batchUpdateStatus(updates);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("updatedCount", count));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/metadata/search")
    public ResponseEntity<?> filterByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value) {
        try {
            List<Shipment> results = shipmentService.filterByMetadata(key, operator, value);
            return ResponseEntity.ok(results);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @GetMapping("/history")
    public ResponseEntity<List<Shipment>> getShipmentHistory(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(shipmentService.getShipmentsInDateRange(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59),
                status));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/analytics")
    public ResponseEntity<ShippingAnalyticsDTO> getShipmentAnalytics(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate){
        return ResponseEntity.ok(shipmentService.getShippingAnalytics(startDate, endDate));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @PostMapping("/{id}/tracking")
    public ResponseEntity<ShipmentTrackingEvent> recordTrackingEvent(@PathVariable Long id, @RequestBody ShipmentTrackingRequestDTO request
    ) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(shipmentService.recordTrackingEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/{id}/tracking") //[cite: 1]
    public ResponseEntity<List<ShipmentTrackingDTO>> getTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        return ResponseEntity.ok(shipmentService.getTrackingTimeline(id, startTime, endTime));
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/order/{orderId}/active")
    public ResponseEntity<ShipmentDTO> getActiveShipment(@PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentService.getActiveShipment(orderId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/order/{orderId}/ids")
    public ResponseEntity<List<Long>> getShipmentIdsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentIdsByOrderId(orderId));
    }
}
