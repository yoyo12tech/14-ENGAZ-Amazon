package com.team14.amazon.contracts.feign;

import com.team14.amazon.contracts.dto.ShippingAddressDTO;
import com.team14.amazon.contracts.dto.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface UserServiceClient {



    //Dont forget to update the userDTO class
    @GetMapping("/api/users/{id}")
    UserDTO getUser(@PathVariable("id") Long userId);


    @GetMapping("/api/users/{userId}/addresses/{addressId}")
    ShippingAddressDTO getShippingAddress(@PathVariable Long userId,
                                          @PathVariable Long addressId);
}
