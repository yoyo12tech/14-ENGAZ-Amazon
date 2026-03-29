package com.team14.amazon.user.controller;

import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.service.ShippingAddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class UserAddressController {

    private final ShippingAddressService shippingAddressService;

    public UserAddressController(ShippingAddressService shippingAddressService) {
        this.shippingAddressService = shippingAddressService;
    }

    @GetMapping
    public ResponseEntity<List<ShippingAddress>> getUserAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(shippingAddressService.getAddressesByUserId(userId));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ShippingAddress> getAddressById(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        return shippingAddressService.getAddressByIdAndUserId(addressId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ShippingAddress> createAddress(
            @PathVariable Long userId,
            @RequestBody ShippingAddress address) {
        return shippingAddressService.createAddressForUser(userId, address)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.notFound().build()); // 404 if user doesn't exist
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ShippingAddress> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @RequestBody ShippingAddress updatedAddress) {
        return shippingAddressService.updateAddress(userId, addressId, updatedAddress)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        if (shippingAddressService.deleteAddress(userId, addressId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
