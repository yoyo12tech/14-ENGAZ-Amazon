package com.team14.amazon.shipping.controller;

import com.team14.amazon.shipping.dto.NearbyShipmentDTO;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.shipping.dto.BatchStatusUpdateRequest;

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

    @DeleteMapping("/purge")
    public ResponseEntity<Integer> purgeOldShipments(@RequestParam int olderThanDays) {
        return ResponseEntity.ok(shipmentService.purgeShipments(olderThanDays));
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

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyShipmentDTO>> findNearbyShipments(@RequestParam Double lat, @RequestParam Double lon, @RequestParam Double radiusKm){
        return ResponseEntity.ok(shipmentService.findNearbyShipments(lat, lon, radiusKm));
}
    @PostMapping("/order/{orderId}")
    public ResponseEntity<?> createShipmentForOrder(@PathVariable Long orderId, @RequestBody Shipment shipment){
        try{
            Shipment created = shipmentService.createShipment(orderId, shipment);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not save Shipment!");
        }
    }


    @GetMapping("order/{orderId}/latest")
    public Shipment getLatestShipmentForAnOrder(@PathVariable Long orderId){
        return shipmentService.getLatestShipmentForOrder(orderId);
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
