package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.dto.DelayedShipmentDTO;
import com.team14.amazon.shipping.dto.CarrierSummaryDTO;
import com.team14.amazon.shipping.dto.NearbyShipmentDTO;
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

    @Transactional
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

    public List<NearbyShipmentDTO> findNearbyShipments(Double lat, Double lon, Double radiusKm ){
        return shipmentRepository.findByStatusNative(Shipment.Status.OUT_FOR_DELIVERY.name())
                .stream()
                .filter( s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    Double distance = Math.sqrt(
                            Math.pow(s.getLatitude() - lat, 2) + Math.pow(s.getLongitude() - lon, 2)
                    ) * 111;
//                    return new NearbyShipmentDTO(
//                            s.getId(),
//                            s.getOrderId(),
//                            s.getCarrier(),
//                            s.getTrackingNumber(),
//                            s.getLatitude(),
//                            s.getLongitude(),
//                            distance
//                    );
                    return NearbyShipmentDTO
                            .builder()
                            .shipmentId(s.getId())
                            .orderId(s.getOrderId())
                            .carrier(s.getCarrier())
                            .trackingNumber(s.getTrackingNumber())
                            .latitude(s.getLatitude())
                            .longitude(s.getLongitude())
                            .distanceKm(distance)
                            .build();

                })
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyShipmentDTO::getDistanceKm))
                .collect(Collectors.toList());
    }
    public Shipment createShipment(Long orderId, Shipment shipment){
        if(!shipmentRepository.existsOrderById(orderId)){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The Order does not exist!");
        }
        shipment.setOrderId(orderId);
        shipment.setStatus(Shipment.Status.PROCESSING);
        shipment.setLastUpdate(LocalDateTime.now());

        return shipmentRepository.save(shipment);
  
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
    }    public List<Shipment> filterByMetadata(String key, String operator, String value) {
        return switch (operator) {
            case "eq" -> shipmentRepository.findByMetadataEq(key, value);
            case "gt" -> {
                try {
                    yield shipmentRepository.findByMetadataGt(key, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Value must be numeric for 'gt' operator");
                }
            }
            case "lt" -> {
                try {
                    yield shipmentRepository.findByMetadataLt(key, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Value must be numeric for 'lt' operator");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid operator. Must be one of: eq, gt, lt");
        };
    }

    public List<Shipment> getShipmentsInDateRange(LocalDateTime startDate, LocalDateTime endDate, String status) {
        String resolvedStatus = (status == null || status.isBlank()) ? null : status;
        return shipmentRepository.findByDateRangeAndStatus(startDate, endDate, resolvedStatus);
    }


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

    public List<DelayedShipmentDTO> getDelayedShipments(Integer maxAttempts) {
        return shipmentRepository.findDelayedShipments(maxAttempts).stream()
                .map(row -> new DelayedShipmentDTO(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        (String) row[3],
                        (java.time.LocalDateTime) row[4],
                        ((Number) row[5]).longValue(),
                        (Integer) row[6]
                )).toList();
    }
}
