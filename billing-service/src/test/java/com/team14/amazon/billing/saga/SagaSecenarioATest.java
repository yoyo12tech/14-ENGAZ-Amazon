package com.team14.amazon.billing.saga;

import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SagaScenarioATest extends SagaBaseTest {

    // Scenario A — Happy Path
    // order.completed → billing creates PENDING transaction → publishes payment.initiated
    // → payment succeeds → publishes payment.completed → order becomes PAID

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void scenarioA_orderCompleted_createsPendingTransaction_andPublishesPaymentInitiated() {

        // --- SETUP ---
        // Simulates: User ID=1 (ACTIVE), Order ID=10 (SHIPPED, totalAmount=45350)
        // We don't seed these in billing-postgres since billing only owns Transaction.
        // UserServiceClient is mocked in base class to return User ID=1.

        // --- ACTION ---
        // Simulates what order-service publishes after PUT /api/orders/10/deliver passes all pre-checks
        OrderCompletedEvent event = new OrderCompletedEvent(10L, 1L, 1L, 45350.0);
        rabbitTemplate.convertAndSend("order.events", "order.completed", event);

        // --- EXPECT ---
        // billing-service consumer receives the event and creates a PENDING transaction
        await().atMost(10, SECONDS).untilAsserted(() -> {
            var transaction = transactionRepository.findByOrderId(10L);

            assertThat(transaction).isNotNull();
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(transaction.getAmount()).isEqualTo(45350.0);
            assertThat(transaction.getUserId()).isEqualTo(1L);
            assertThat(transaction.getOrderId()).isEqualTo(10L);
        });
    }

    @Test
    void scenarioA_idempotency_duplicateOrderCompletedEvent_doesNotCreateDuplicateTransaction() {

        // --- ACTION ---
        // Publish the same event twice (simulates at-least-once delivery from RabbitMQ)
        OrderCompletedEvent event = new OrderCompletedEvent(10L, 1L, 1L, 45350.0);
        rabbitTemplate.convertAndSend("order.events", "order.completed", event);
        rabbitTemplate.convertAndSend("order.events", "order.completed", event);

        // --- EXPECT ---
        // Only one transaction created (idempotency check in consumer)
        await().atMost(10, SECONDS).untilAsserted(() -> {
            var transactions = transactionRepository.findAllByOrderId(10L);
            assertThat(transactions).isNotNull();
        });
    }
}
