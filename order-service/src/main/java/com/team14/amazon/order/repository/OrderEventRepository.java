package com.team14.amazon.order.repository;

import com.team14.amazon.order.model.mongo.OrderEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends MongoRepository<OrderEvent, String> {
    List<OrderEvent> findByOrderIdOrderByTimestampDesc(Long orderId);
}
