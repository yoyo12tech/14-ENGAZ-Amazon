package com.team14.amazon.user.controller;

import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.service.ShippingAddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/shipping-addresses")
public class ShippingAddressController {
    private final ShippingAddressService shippingAddressService;

    // Constructor injection (Best practice, no @Autowired needed)
    public ShippingAddressController(ShippingAddressService shippingAddressService) {
        this.shippingAddressService = shippingAddressService;
    }

    // READ: Get all addresses for a specific user
    @GetMapping
    public ResponseEntity<List<ShippingAddress>> getAllAddresses(@PathVariable Long userId) {
        List<ShippingAddress> addresses = shippingAddressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(addresses);
    }

    // READ: Get a specific address for a specific user
    @GetMapping("/{addressId}")
    public ResponseEntity<ShippingAddress> getAddressById(
            @PathVariable Long userId,
            @PathVariable Long addressId) {

        return shippingAddressService.getAddressByIdAndUserId(addressId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE: Add a new address for a user
    @PostMapping
    public ResponseEntity<ShippingAddress> createAddress(
            @PathVariable Long userId,
            @RequestBody ShippingAddress address) {

        return shippingAddressService.createAddressForUser(userId, address)
                .map(createdAddress -> ResponseEntity.status(HttpStatus.CREATED).body(createdAddress))
                .orElse(ResponseEntity.notFound().build()); // 404 if the user doesn't exist
    }

    // UPDATE: Modify an existing address
    @PutMapping("/{addressId}")
    public ResponseEntity<ShippingAddress> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @RequestBody ShippingAddress addressDetails) {

        return shippingAddressService.updateAddress(userId, addressId, addressDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE: Remove an address
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {

        boolean isDeleted = shippingAddressService.deleteAddress(userId, addressId);
        if (isDeleted) {
            return ResponseEntity.noContent().build(); // 204 No Content is standard for successful deletions
        }
        return ResponseEntity.notFound().build();
    }
}
