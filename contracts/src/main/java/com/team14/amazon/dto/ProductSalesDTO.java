package com.team14.amazon.dto;

public record ProductSalesDTO(    
          Long productId ,
          String name ,
          Long totalUnitsSold ,
          Double totalRevenue ,
          Double averageSellingPrice)
{}
