package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
}
