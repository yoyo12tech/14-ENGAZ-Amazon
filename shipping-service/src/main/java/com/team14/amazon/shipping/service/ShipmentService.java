package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.team14.amazon.shipping.dto.BatchStatusUpdateRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public Optional<Shipment> getShipmentById(Long id) {
        return shipmentRepository.findById(id);
    }

    public Shipment createShipment(Shipment shipment) {
            if (shipment.getTrackingNumber() != null && 
                shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()).isPresent()) {
                throw new IllegalArgumentException("Tracking number already exists.");
            }

            shipment.setLastUpdate(LocalDateTime.now());
            
            if (shipment.getStatus() == null) {
                shipment.setStatus(Shipment.Status.PROCESSING);
            }
            
            return shipmentRepository.save(shipment);
    }
    public Optional<Shipment> updateShipment(Long id, Shipment updatedShipment) {
        return shipmentRepository.findById(id).map(existingShipment -> {
            existingShipment.setOrderId(updatedShipment.getOrderId());
            existingShipment.setCarrier(updatedShipment.getCarrier());
            existingShipment.setStatus(updatedShipment.getStatus());
            existingShipment.setLatitude(updatedShipment.getLatitude());
            existingShipment.setLongitude(updatedShipment.getLongitude());
            existingShipment.setEstimatedDelivery(updatedShipment.getEstimatedDelivery());
            existingShipment.setActualDelivery(updatedShipment.getActualDelivery());
            existingShipment.setMetadata(updatedShipment.getMetadata());
            existingShipment.setLastUpdate(LocalDateTime.now());

            return shipmentRepository.save(existingShipment);
        });
    }

    public int purgeShipments(int olderThanDays) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(olderThanDays);
        return shipmentRepository.purgeOldShipments(cutoff);
    }

    public boolean deleteShipment(Long id) {
        if (shipmentRepository.existsById(id)) {
            shipmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Shipment getLatestShipmentForOrder(Long orderId){
        if(!shipmentRepository.existsByOrderId(orderId)){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The Order has no shipping yet!");
        }
        return shipmentRepository.findLatestShipmentByOrderId(orderId)
        .orElseThrow(()-> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "The Order provided is still being processed!"));
        }
  
  
    @Transactional
    public int batchUpdateStatus(List<BatchStatusUpdateRequest> updates) {
        List<Shipment> toSave = new ArrayList<>();

        for (BatchStatusUpdateRequest req : updates) {
            // Validate coordinates
            if (req.getLatitude() == null || req.getLatitude() < -90 || req.getLatitude() > 90) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Latitude must be between -90 and 90");
            }
            if (req.getLongitude() == null || req.getLongitude() < -180 || req.getLongitude() > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Longitude must be between -180 and 180");
            }

            // Find shipment — 404 if not found
            Shipment shipment = shipmentRepository.findById(req.getShipmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Shipment not found: " + req.getShipmentId()));

            shipment.setStatus(Shipment.Status.valueOf(req.getStatus()));
            shipment.setLatitude(req.getLatitude());
            shipment.setLongitude(req.getLongitude());
            shipment.setLastUpdate(LocalDateTime.now());

            toSave.add(shipment);
        }

        shipmentRepository.saveAll(toSave);
        return toSave.size();
    }
}
