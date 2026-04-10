package com.team14.amazon.user.controller;

import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.service.ShippingAddressService;
import com.team14.amazon.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;
    private final UserService userService; // Added UserService

    // Updated constructor to inject both services
    public ShippingAddressController(ShippingAddressService shippingAddressService, UserService userService) {
        this.shippingAddressService = shippingAddressService;
        this.userService = userService;
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
                .orElse(ResponseEntity.notFound().build());
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

    // ==========================================
    // MILESTONE 1 FEATURES
    // ==========================================

    /**
     * [S1-F7] Set Default Shipping Address
     */
    @PutMapping("/{addressId}/default")
    public ResponseEntity<User> setDefaultAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {

        User updatedUser = userService.setDefaultShippingAddress(userId, addressId);
        return ResponseEntity.ok(updatedUser);
    }
}