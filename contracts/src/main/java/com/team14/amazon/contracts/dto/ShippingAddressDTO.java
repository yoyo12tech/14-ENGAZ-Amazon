package com.team14.amazon.contracts.dto;

public record ShippingAddressDTO(
        Long id,
        Long userId,
        String street,
        String city,
        String state,
        String country,
        String postalCode
) {}