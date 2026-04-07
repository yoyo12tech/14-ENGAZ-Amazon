package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.dto.NearbyShipmentDTO;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<NearbyShipmentDTO> findNearbyShipments(Double lat, Double lon, Double radiusKm ){
        return shipmentRepository.findByStatus(Shipment.Status.OUT_FOR_DELIVERY)
                .stream()
                .filter( s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    Double distance = Math.sqrt(
                            Math.pow(s.getLatitude() - lat, 2) + Math.pow(s.getLongitude() - lon, 2)
                    ) * 111;
                    return new NearbyShipmentDTO(
                            s.getId(),
                            s.getOrderId(),
                            s.getCarrier(),
                            s.getTrackingNumber(),
                            s.getLatitude(),
                            s.getLongitude(),
                            distance
                    );

                })
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyShipmentDTO::getDistanceKm))
                .collect(Collectors.toList());
    }
}
