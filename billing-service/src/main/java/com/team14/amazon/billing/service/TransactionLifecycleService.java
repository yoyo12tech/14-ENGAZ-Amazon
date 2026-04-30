package com.team14.amazon.billing.service;

import com.team14.amazon.billing.cache.BillingCacheNames;
import com.team14.amazon.billing.dto.LifecycleEventDTO;
import com.team14.amazon.billing.model.Source;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.repository.mongodb.TransactionAuditEventRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.*;

@Service
public class TransactionLifecycleService {

    private final TransactionRepository transactionRepository;
    private final TransactionAuditEventRepository transactionAuditEventRepository;
    private final MongoTemplate mongoTemplate;
    private final JdbcTemplate shippingJdbcTemplate;

    public TransactionLifecycleService(
            TransactionRepository transactionRepository,
            TransactionAuditEventRepository transactionAuditEventRepository,
            MongoTemplate mongoTemplate,
            @Qualifier("shippingJdbcTemplate") JdbcTemplate shippingJdbcTemplate) {
        this.transactionRepository = transactionRepository;
        this.transactionAuditEventRepository = transactionAuditEventRepository;
        this.mongoTemplate = mongoTemplate;
        this.shippingJdbcTemplate = shippingJdbcTemplate;
    }

    @Cacheable(value = BillingCacheNames.TRANSACTION_LIFECYCLE, key = "#transactionId")
    public List<LifecycleEventDTO> getTransactionLifecycle(Long transactionId, Long callerUid, String callerRole) {

        // Step 1 - JWT validation is handled by JwtAuthenticationFilter upstream,
        //          callerUid and callerRole are extracted from SecurityContext by the controller

        // Step 2 - Find transaction by ID in PostgreSQL, throw 404 if not found
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found with id: " + transactionId));

        Long orderId = transaction.getOrderId();

        // Step 3 - Ownership check: caller must be the transaction owner or an ADMIN
        if (!transaction.getUserId().equals(callerUid) && !"ADMIN".equals(callerRole)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: you do not own this transaction");
        }

        // Step 4 - Cross-service native SQL: fetch shipment IDs from shipping service's
        //          PostgreSQL database where order_id matches this transaction's orderId
        List<Long> shipmentIds = shippingJdbcTemplate.queryForList(
                "SELECT id FROM shipments WHERE order_id = ?",
                Long.class,
                orderId
        );

        // Step 5a - Query transaction_audit_trail: all events for this transactionId,
        //           tag each with source = "TRANSACTION"
        List<LifecycleEventDTO> transactionEvents = transactionAuditEventRepository
                .findByTransactionId(transactionId)
                .stream()
                .map(e -> new LifecycleEventDTO(e.getTimestamp(), Source.TRANSACTION, e.getAction(), e.getDetails()))
                .toList();

        // Step 5b - Query order_events: all events for this orderId,
        //           tag each with source = "ORDER"
        List<LifecycleEventDTO> orderEvents = mongoTemplate
                .find(Query.query(Criteria.where("orderId").is(orderId)), Document.class, "order_events")
                .stream()
                .map(doc -> new LifecycleEventDTO(
                        doc.get("timestamp", Date.class).toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        Source.ORDER,
                        doc.getString("action"),
                        doc.get("details", Map.class)
                ))
                .toList();

        // Step 5c - Query shipment_events: all events whose shipmentId is in the list from Step 4,
        //           tag each with source = "SHIPMENT"
        // Return empty list early if no shipments exist for this order
        List<LifecycleEventDTO> shipmentEvents = new ArrayList<>();
        if (!shipmentIds.isEmpty()) {
            shipmentEvents = mongoTemplate
                    .find(Query.query(Criteria.where("shipmentId").in(shipmentIds)), Document.class, "shipment_events")
                    .stream()
                    .map(doc -> new LifecycleEventDTO(
                            doc.get("timestamp", Date.class).toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            Source.SHIPMENT,
                            doc.getString("action"),
                            doc.get("details", Map.class)
                    ))
                    .toList();
        }

        // Step 6 - Merge all three lists and sort by timestamp ascending (chronological order)
        List<LifecycleEventDTO> timeline = new ArrayList<>();
        timeline.addAll(transactionEvents);
        timeline.addAll(orderEvents);
        timeline.addAll(shipmentEvents);

        timeline.sort(Comparator.comparing(LifecycleEventDTO::timestamp));

        // Step 7 - Caching is handled by @Cacheable above (10 min TTL via BillingCacheNames.TRANSACTION_LIFECYCLE)

        // Step 8 - Return the merged sorted timeline, empty list naturally returned if no events exist
        return timeline;
    }
}
