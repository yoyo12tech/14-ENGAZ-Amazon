package com.team14.amazon.shipping.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record OrderDTO(
        Long id,
        Long userId,
        Long shippingAddressId,
        String status,
        Double totalAmount,
        LocalDateTime orderedAt,
        LocalDateTime deliveredAt
) {}

