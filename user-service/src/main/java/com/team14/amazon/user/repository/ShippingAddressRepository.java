package com.team14.amazon.user.repository;

import com.team14.amazon.user.model.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByUserId(Long userId);
    Optional<ShippingAddress> findByIdAndUserId(Long id, Long userId);
}
