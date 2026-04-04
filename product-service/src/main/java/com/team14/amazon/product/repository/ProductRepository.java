package com.team14.amazon.product.repository;


import com.team14.amazon.product.model.Product;
import com.team14.amazon.product.model.ProductStatus;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    //Below are extra methods we might need later on......

    List<Product> findByCategory(String category);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByBrand(String brand);
}