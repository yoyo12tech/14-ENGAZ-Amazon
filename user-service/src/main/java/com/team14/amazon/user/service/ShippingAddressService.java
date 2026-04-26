package com.team14.amazon.user.service;

import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.repository.ShippingAddressRepository;
import com.team14.amazon.user.repository.UserRepository;
import com.team14.amazon.user.observer.EntityObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;
    private final UserRepository userRepository;
    private final List<EntityObserver> observers = new ArrayList<>();

    public ShippingAddressService(ShippingAddressRepository shippingAddressRepository, 
                                 UserRepository userRepository,
                                 List<EntityObserver> observers) {
        this.shippingAddressRepository = shippingAddressRepository;
        this.userRepository = userRepository;
        this.observers.addAll(observers);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    public List<ShippingAddress> getAddressesByUserId(Long userId) {
        return shippingAddressRepository.findByUserId(userId);
    }

    public Optional<ShippingAddress> getAddressByIdAndUserId(Long addressId, Long userId) {
        return shippingAddressRepository.findByIdAndUserId(addressId, userId);
    }

    public Optional<ShippingAddress> createAddressForUser(Long userId, ShippingAddress address) {
        return userRepository.findById(userId).map(user -> {
            address.setUser(user);
            ShippingAddress savedAddress = shippingAddressRepository.save(address);

            // Log ADDRESS_CREATED event
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("userId", userId);
            eventPayload.put("timestamp", LocalDateTime.now());
            eventPayload.put("details", Map.of("addressId", savedAddress.getId()));
            notifyObservers("ADDRESS_CREATED", eventPayload);

            return savedAddress;
        });
    }

    public Optional<ShippingAddress> updateAddress(Long userId, Long addressId, ShippingAddress updatedAddress) {
        return shippingAddressRepository.findByIdAndUserId(addressId, userId).map(existingAddress -> {
            existingAddress.setLabel(updatedAddress.getLabel());
            existingAddress.setStreetAddress(updatedAddress.getStreetAddress());
            existingAddress.setCity(updatedAddress.getCity());
            existingAddress.setCountry(updatedAddress.getCountry());
            existingAddress.setZipCode(updatedAddress.getZipCode());
            existingAddress.setDefault(updatedAddress.isDefault());
            existingAddress.setMetadata(updatedAddress.getMetadata());
            ShippingAddress savedAddress = shippingAddressRepository.save(existingAddress);

            // Log ADDRESS_UPDATED event
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("userId", userId);
            eventPayload.put("timestamp", LocalDateTime.now());
            eventPayload.put("details", Map.of("addressId", savedAddress.getId()));
            notifyObservers("ADDRESS_UPDATED", eventPayload);

            return savedAddress;
        });
    }

    public boolean deleteAddress(Long userId, Long addressId) {
        Optional<ShippingAddress> address = shippingAddressRepository.findByIdAndUserId(addressId, userId);
        if (address.isPresent()) {
            shippingAddressRepository.delete(address.get());

            // Log ADDRESS_DELETED event
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("userId", userId);
            eventPayload.put("timestamp", LocalDateTime.now());
            eventPayload.put("details", Map.of("addressId", addressId));
            notifyObservers("ADDRESS_DELETED", eventPayload);

            return true;
        }
        return false;
    }
}
