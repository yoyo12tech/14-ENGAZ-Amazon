package com.team14.amazon.product.repository.mongodb;

import com.team14.amazon.product.model.mongodb.ProductEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductEventRepository extends MongoRepository<ProductEvent, String> {
    List<ProductEvent> findByProductId(Long productId);

    List<ProductEvent> findByAction(String action);
}

