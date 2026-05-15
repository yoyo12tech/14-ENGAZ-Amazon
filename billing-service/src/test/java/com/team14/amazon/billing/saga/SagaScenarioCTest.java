package com.team14.amazon.billing.saga;

import com.team14.amazon.contracts.events.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SagaScenarioCTest extends SagaBaseTest {

    // Scenario C — Pre-check Failure (no active shipment)
    // order-service calls GET /api/shipments/order/10/active → 404
    // → order-service aborts, never publishes order.completed
    // → billing-service receives nothing → no transaction created

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void scenarioC_noOrderCompletedPublished_noTransactionCreated() {

        // --- SETUP ---
        // No event is published — order-service aborted because shipping pre-check failed.
        // We verify billing-service correctly has no transaction for orderId=10.

        // --- ACTION ---
        // Deliberately do NOT publish order.completed (simulating aborted delivery)

        // --- EXPECT ---
        // After a reasonable wait, billing-postgres still has no transaction for order 10
        await()
                .pollDelay(3, TimeUnit.SECONDS)  // wait to confirm nothing arrives
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var transaction = transactionRepository.findByOrderId(10L);
                    assertThat(transaction).isNull();
                });
    }

    @Test
    void scenarioC_unrelatedEvent_doesNotCreateTransactionForOrder10() {

        // --- ACTION ---
        // Publish an order.completed for a DIFFERENT order (orderId=99)
        // to confirm billing-service doesn't mix up orders
        OrderCompletedEvent unrelatedEvent = new OrderCompletedEvent(99L, 2L, 1L, 100.0);
        rabbitTemplate.convertAndSend("order.events", "order.completed", unrelatedEvent);

        // --- EXPECT ---
        // Transaction created for order 99, but NOT for order 10
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var transactionFor99 = transactionRepository.findByOrderId(99L);
            var transactionFor10 = transactionRepository.findByOrderId(10L);

            assertThat(transactionFor99).isNotNull();
            assertThat(transactionFor10).isNull();
        });
    }
}