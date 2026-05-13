package com.team14.amazon.dto;

public record TopProductDTO(
          Long productId ,
          String name ,
          Double rating ,
          Long totalSales
) {}