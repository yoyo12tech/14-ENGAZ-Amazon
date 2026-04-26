package com.team14.amazon.billing.repository.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.team14.amazon.billing.model.mongodb.TransactionAuditEvent;

public interface TransactionAuditEventRepository extends MongoRepository<TransactionAuditEvent, String> {
}
