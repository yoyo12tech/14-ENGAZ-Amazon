package com.team14.amazon.dto;

public record TopBuyerDTO (
          Long userId,
          String name,
          Double totalSpent,
          Long orderCount
)
{ }
