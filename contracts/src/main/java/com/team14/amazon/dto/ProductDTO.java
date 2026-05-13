package com.team14.amazon.dto;
public record ProductDTO(
        Long id,
        String name,
        String description,
        Double price,
        String category,
        String brand,
        Integer stockQuantity,
        String status,
        Double rating
) {}

