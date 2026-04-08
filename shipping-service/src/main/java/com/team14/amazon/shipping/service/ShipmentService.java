package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.dto.CarrierSummaryDTO;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

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

    // S4-F8
    public CarrierSummaryDTO getCarrierSummary(String carrier, java.time.LocalDate start, java.time.LocalDate end) {
        // Get the list of results
        List<Object[]> results = shipmentRepository.getCarrierSummary(carrier, start, end);

        // If list is empty, the carrier had no shipments in this range
        if (results == null || results.isEmpty()) {
            return new CarrierSummaryDTO(carrier, 0L, 0L, 0.0, 0.0);
        }

        // Since we grouped by carrier, there is only one row in the list
        Object[] row = results.getFirst();

        return new CarrierSummaryDTO(
                row[0] != null ? row[0].toString() : carrier, // carrier name
                row[1] != null ? ((Number) row[1]).longValue() : 0L, // totalShipments
                row[2] != null ? ((Number) row[2]).longValue() : 0L, // deliveredCount
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0, // averageDeliveryDays
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0  // onTimeRate
        );
    }
}
