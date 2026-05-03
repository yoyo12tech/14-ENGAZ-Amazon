package com.team14.amazon.shipping.repository.mongo;

import com.team14.amazon.shipping.model.mongo.ShipmentEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentEventRepository extends MongoRepository<ShipmentEvent, String> {
}