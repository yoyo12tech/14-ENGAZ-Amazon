package com.team14.amazon.shipping.service;

import com.team14.amazon.shipping.adapter.ObjectArrayDtoAdapter;
import com.team14.amazon.shipping.dto.*;
import com.team14.amazon.shipping.model.Shipment;
import com.team14.amazon.shipping.model.mongo.ShipmentEvent;
import com.team14.amazon.shipping.observer.MongoEventLogger;
import com.team14.amazon.shipping.model.cassandra.ShipmentTrackingEvent;
import com.team14.amazon.shipping.repository.ShipmentRepository;
import com.team14.amazon.shipping.repository.mongo.ShipmentEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.shipping.observer.EntityObserver;
import com.team14.amazon.shipping.observer.MongoEventLogger;
import com.team14.amazon.shipping.repository.cassandra.ShipmentTrackingEventRepository;

import java.time.LocalDate;
import java.util.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final MongoEventLogger mongoEventLogger;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final CachedServices cachedServices;
    private final ShipmentEventRepository shipmentEventRepository;
    private final ShipmentTrackingEventRepository trackingRepository;
    

    public ShipmentService(ShipmentRepository shipmentRepository, ShipmentTrackingEventRepository trackingRepository,  MongoEventLogger mongoEventLogger, CachedServices cachedServices, ShipmentEventRepository shipmentEventRepository) {
        this.shipmentRepository = shipmentRepository;
        this.mongoEventLogger = mongoEventLogger;
        this.cachedServices = cachedServices;
        this.shipmentEventRepository = shipmentEventRepository;
        this.trackingRepository = trackingRepository;

        register(mongoEventLogger);
    }

    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @Cacheable(value = "shipping-service::shipment", key = "#id")
    public Optional<Shipment> getShipmentById(Long id) {
        return shipmentRepository.findById(id);
    }

    @Caching(evict = {
            @CacheEvict(value = "shipping-service::S4-F1", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F3", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F9", allEntries = true)
    })
    public Shipment createShipment(Shipment shipment) {
            if (shipment.getTrackingNumber() != null && 
                shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()).isPresent()) {
                throw new IllegalArgumentException("Tracking number already exists.");
            }

            shipment.setLastUpdate(LocalDateTime.now());
            
            if (shipment.getStatus() == null) {
                shipment.setStatus(Shipment.Status.PROCESSING);
            }

        Shipment saved = shipmentRepository.save(shipment);
        notifyObservers("SHIPMENT_CREATED", saved.getId(), Map.of(
                "orderId", saved.getOrderId(),
                "carrier", saved.getCarrier(),
                "status", saved.getStatus().name()
        ));
        return saved;
    }
    @Caching(evict = {
            @CacheEvict(value = "shipping-service::shipment", key = "#id"),
            @CacheEvict(value = "shipping-service::S4-F3", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F6", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F8", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F9", allEntries = true)
    })
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

            Shipment saved = shipmentRepository.save(existingShipment);
            notifyObservers("SHIPMENT_UPDATED", saved.getId(), Map.of(
                    "orderId", saved.getOrderId(),
                    "carrier", saved.getCarrier(),
                    "status", saved.getStatus().name()
            ));
            return saved;
        });
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String action, Long shipmentId, Map<String, Object> details) {
        for (EntityObserver observer : observers) {
            observer.onEvent(action, shipmentId, details);
        }
    }
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "shipping-service::S4-F6", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F9", allEntries = true)
    })
    public int purgeShipments(int olderThanDays) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(olderThanDays);
        return shipmentRepository.purgeOldShipments(cutoff);
    }

    @Caching(evict = {
            @CacheEvict(value = "shipping-service::shipment", key = "#id"),
            @CacheEvict(value = "shipping-service::S4-F3", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F6", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F9", allEntries = true)
    })
    public boolean deleteShipment(Long id) {
        if (shipmentRepository.existsById(id)) {
            shipmentRepository.deleteById(id);
            notifyObservers("SHIPMENT_DELETED", id, Map.of());
            return true;
        }
        return false;
    }

    @Cacheable(value = "shipping-service::S4-F3", key = "#lat + '::' + #lon + '::' + #radiusKm")
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
    @Caching(evict = {
            @CacheEvict(value = "shipping-service::S4-F1", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F3", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F9", allEntries = true)
    })
    public Shipment createShipment(Long orderId, Shipment shipment){
        if(!shipmentRepository.existsOrderById(orderId)){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The Order does not exist!");
        }
        shipment.setOrderId(orderId);
        shipment.setStatus(Shipment.Status.PROCESSING);
        shipment.setLastUpdate(LocalDateTime.now());

        Shipment saved = shipmentRepository.save(shipment);
        notifyObservers("SHIPMENT_CREATED", saved.getId(), Map.of(
                "orderId", saved.getOrderId(),
                "carrier", saved.getCarrier(),
                "status", saved.getStatus().name()
        ));
        return saved;
  
     }
    @Cacheable(value = "shipping-service::S4-F1", key = "#orderId")
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
    @Caching(evict = {
            @CacheEvict(value = "shipping-service::S4-F3", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F6", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F8", allEntries = true),
            @CacheEvict(value = "shipping-service::S4-F9", allEntries = true)
    })
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
        notifyObservers("BATCH_STATUS_UPDATED", null, Map.of(
                "updatedCount", toSave.size()
        ));
        return toSave.size();
    }
    public List<Shipment> filterByMetadata(String key, String operator, String value) {
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
    @Cacheable(value = "shipping-service::S4-F6", key = "#startDate + '::' + #endDate + '::' + #status")
    public List<Shipment> getShipmentsInDateRange(LocalDateTime startDate, LocalDateTime endDate, String status) {
        String resolvedStatus = (status == null || status.isBlank()) ? null : status;
        return shipmentRepository.findByDateRangeAndStatus(startDate, endDate, resolvedStatus);
    }

    @Cacheable(value = "shipping-service::S4-F8", key = "#carrier + '::' + #start + '::' + #end")
    public CarrierSummaryDTO getCarrierSummary(String carrier, java.time.LocalDate start, java.time.LocalDate end) {
        // Get the list of results
        List<Object[]> results = shipmentRepository.getCarrierSummary(carrier, start, end);

        // If list is empty, the carrier had no shipments in this range
        if (results == null || results.isEmpty()) {
            //return new CarrierSummaryDTO(carrier, 0L, 0L, 0.0, 0.0);
            return CarrierSummaryDTO.builder()
                    .carrier(carrier)
                    .totalShipments(0L)
                    .deliveredCount(0L)
                    .averageDeliveryDays(0.0)
                    .onTimeRate(0.0)
                    .build();

        }

        // Since we grouped by carrier, there is only one row in the list
        Object[] row = results.getFirst();

//        return new CarrierSummaryDTO(
//                row[0] != null ? row[0].toString() : carrier, // carrier name
//                row[1] != null ? ((Number) row[1]).longValue() : 0L, // totalShipments
//                row[2] != null ? ((Number) row[2]).longValue() : 0L, // deliveredCount
//                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0, // averageDeliveryDays
//                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0  // onTimeRate
//        );
//        return CarrierSummaryDTO.builder()
//                .carrier(row[0] != null ? row[0].toString() : carrier)
//                .totalShipments(row[1] != null ? ((Number) row[1]).longValue() : 0L)
//                .deliveredCount(row[2] != null ? ((Number) row[2]).longValue() : 0L)
//                .averageDeliveryDays(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
//                .onTimeRate(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
//                .build();
        return ObjectArrayDtoAdapter.adaptToCarrierSummaryDTO(row, carrier);
    }
    @Cacheable(value = "shipping-service::S4-F9", key = "#maxAttempts")
    public List<DelayedShipmentDTO> getDelayedShipments(Integer maxAttempts) {
//        return shipmentRepository.findDelayedShipments(maxAttempts).stream()
//                .map(row -> DelayedShipmentDTO.builder()
//                        .shipmentId(((Number) row[0]).longValue())
//                        .orderId(((Number) row[1]).longValue())
//                        .carrier((String) row[2])
//                        .trackingNumber((String) row[3])
//                        .estimatedDelivery((LocalDateTime) row[4])
//                        .daysOverdue(((Number) row[5]).longValue())
//                        .deliveryAttempts((Integer) row[6])
//                        .build()
//                ).toList();
        return shipmentRepository.findDelayedShipments(maxAttempts).stream()
                .map(ObjectArrayDtoAdapter::adaptToDelayedShipmentDTO)
                .toList();
    }



//    public ShippingAnalyticsDTO getShippingAnalytics(LocalDate start, LocalDate end) {
//        if (start.isAfter(end)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start Date Cannot be After End Date");
//        }
//        mongoEventLogger.onEvent("ANALYTICS_VIEWED", null, Map.of(
//                "startDate", start.toString(),
//                "endDate", end.toString()
//        ));
//        return cachedServices.getShippingAnalyticsCached(start, end);
//    }

    public ShippingAnalyticsDTO getShippingAnalytics(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        try {
            ShipmentEvent log = new ShipmentEvent(
                    null,
                    "ANALYTICS_VIEWED",
                    Map.of(
                            "startDate", startDate.toString(),
                            "endDate", endDate.toString()
                    )
            );
            shipmentEventRepository.save(log);
        } catch (Exception e) {
            System.err.println("Failed to log analytics view to MongoDB: " + e.getMessage());
        }

        return cachedServices.getShippingAnalyticsCached(startDate, endDate);
    }

//    @Cacheable(value="shipping-service::S4-F10", key = "#start + '::' + #end")
//    public ShippingAnalyticsDTO getShippingAnalyticsCached(LocalDate start, LocalDate end){
//        //cache hit would ignore this section
////        if(start.isAfter(end)){
////            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start Date Cannot be After End Date");
////        }
//        Object[] result = shipmentRepository.getShippingAnalytics(start, end);
//
//        // a null result means no shipping analytics within this time frame (start->end)
//        if (result == null) {
//            return ShippingAnalyticsDTO.builder()
//                    .totalShipments(0L)
//                    .averageDeliveryDays(0.0)
//                    .onTimeRate(0.0)
//                    .shipmentsByStatus(Map.of())
//                    .averageAttempts(0.0)
//                    .build();
//        }
//
//        Map<String, Long> shipmentsByStatus = Map.of(
//                "PROCESSING",       ((Number) result[3]).longValue(),
//                "SHIPPED",          ((Number) result[4]).longValue(),
//                "IN_TRANSIT",       ((Number) result[5]).longValue(),
//                "OUT_FOR_DELIVERY", ((Number) result[6]).longValue(),
//                "DELIVERED",        ((Number) result[7]).longValue(),
//                "RETURNED",         ((Number) result[8]).longValue()
//        );
//
//        return ShippingAnalyticsDTO.builder()
//                .totalShipments(((Number) result[0]).longValue())
//                .averageDeliveryDays(((Number) result[1]).doubleValue())
//                .onTimeRate(((Number) result[2]).doubleValue())
//                .shipmentsByStatus(shipmentsByStatus)
//                .averageAttempts(((Number) result[9]).doubleValue())
//                .build();
//    }


@Caching(evict = {
    @CacheEvict(value = "shipping-service::S4-F12", key = "#shipmentId"),
    @CacheEvict(value = "shipping-service::S4-F10", allEntries = true)
})


public ShipmentTrackingEvent recordTrackingEvent(Long shipmentId, ShipmentTrackingRequestDTO request) {

Shipment shipment = shipmentRepository.findById(shipmentId)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Shipment not found"
        ));

Shipment.Status status;

try {
    status = Shipment.Status.valueOf(request.getStatus());
} catch (Exception e) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid shipment status"
    );
}

shipment.setStatus(status);

if (request.getLatitude() != null) {
    shipment.setLatitude(request.getLatitude());
}

if (request.getLongitude() != null) {
    shipment.setLongitude(request.getLongitude());
}

shipment.setLastUpdate(LocalDateTime.now());

shipmentRepository.save(shipment);

ShipmentTrackingEvent event = new ShipmentTrackingEvent(
    shipment.getId(),
    Instant.now(),
    status,
    shipment.getCarrier(),
    shipment.getTrackingNumber(),
    request.getLatitude(),
    request.getLongitude(),
    request.getNotes()
);

ShipmentTrackingEvent savedEvent = trackingRepository.save(event);

notifyObservers(
        "TRACKING_RECORDED",
        shipment.getId(),
        Map.of(
                "trackingNumber", shipment.getTrackingNumber(),
                "status", status.name(),
                "carrier", shipment.getCarrier(),
                "notes", request.getNotes() != null ? request.getNotes() : ""
        )
);

return savedEvent;
}
}
