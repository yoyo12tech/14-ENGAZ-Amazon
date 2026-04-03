package com.team14.amazon.user.service;

import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.ShippingAddressRepository;
import com.team14.amazon.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;
    private final UserRepository userRepository;

    public ShippingAddressService(ShippingAddressRepository shippingAddressRepository, UserRepository userRepository) {
        this.shippingAddressRepository = shippingAddressRepository;
        this.userRepository = userRepository;
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
            return shippingAddressRepository.save(address);
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
            return shippingAddressRepository.save(existingAddress);
        });
    }

    public boolean deleteAddress(Long userId, Long addressId) {
        Optional<ShippingAddress> address = shippingAddressRepository.findByIdAndUserId(addressId, userId);
        if (address.isPresent()) {
            shippingAddressRepository.delete(address.get());
            return true;
        }
        return false;
    }
}
