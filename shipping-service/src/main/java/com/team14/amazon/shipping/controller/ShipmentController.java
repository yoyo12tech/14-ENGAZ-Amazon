package com.team14.amazon.shipping.controller;

import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team14.amazon.shipping.dto.BatchStatusUpdateRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }


    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable Long id) {
        return shipmentService.getShipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createShipment(@RequestBody Shipment shipment) {
        try {
            Shipment createdShipment = shipmentService.createShipment(shipment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdShipment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shipment> updateShipment(@PathVariable Long id, @RequestBody Shipment updatedShipment) {
        return shipmentService.updateShipment(id, updatedShipment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(@PathVariable Long id) {
        if (shipmentService.deleteShipment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/batch-status")
    public ResponseEntity<?> batchUpdateStatus(@RequestBody List<BatchStatusUpdateRequest> updates) {
        try {
            int count = shipmentService.batchUpdateStatus(updates);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("updatedCount", count));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

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
}
