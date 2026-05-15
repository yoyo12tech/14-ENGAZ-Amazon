package com.team14.amazon.billing.saga;

import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.contracts.events.OrderCancelledEvent;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SagaScenarioBTest extends SagaBaseTest {

    // Scenario B — Payment Failure and Compensation
    // Same setup as A → reach PAYMENT_PENDING
    // → payment fails → transaction=FAILED → payment.failed published
    // → order.cancelled published → transaction=REFUNDED

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void scenarioB_orderCancelled_onPendingTransaction_setsStatusToFailed() {

        // --- SETUP ---
        // Reach PAYMENT_PENDING state: publish order.completed first
        OrderCompletedEvent completedEvent = new OrderCompletedEvent(10L, 1L, 1L, 45350.0);
        rabbitTemplate.convertAndSend("order.events", "order.completed", completedEvent);

        // Wait for PENDING transaction to be created
        await().atMost(10, SECONDS).untilAsserted(() -> {
            var transaction = transactionRepository.findByOrderId(10L);
            assertThat(transaction).isNotNull();
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        });

        // --- ACTION ---
        // Simulates payment failure: order-service publishes order.cancelled
        // with restoredItems=[P1x1, P3x1] and reason="Payment failed"
        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                10L, 1L, List.of(), "Payment failed");
        rabbitTemplate.convertAndSend("order.events", "order.cancelled", cancelledEvent);

        // --- EXPECT ---
        // billing-service consumer sets PENDING → FAILED atomically
        await().atMost(10, SECONDS).untilAsserted(() -> {
            var transaction = transactionRepository.findByOrderId(10L);
            assertThat(transaction).isNotNull();
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
        });
    }

    @Test
    void scenarioB_orderCancelled_onCompletedTransaction_triggersRefund() {

        // --- SETUP ---
        // Seed a COMPLETED transaction directly (simulates already paid order)
        Transaction completed = new Transaction();
        completed.setOrderId(10L);
        completed.setUserId(1L);
        completed.setAmount(45350.0);
        completed.setMethod(TransactionMethod.CREDIT_CARD);
        completed.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(completed);

        // --- ACTION ---
        // order.cancelled published with reason
        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                10L, 1L, List.of(), "Order cancelled by user");
        rabbitTemplate.convertAndSend("order.events", "order.cancelled", cancelledEvent);

        // --- EXPECT ---
        // Refund logic kicks in → transaction becomes REFUNDED
        await().atMost(10, SECONDS).untilAsserted(() -> {
            var transaction = transactionRepository.findByOrderId(10L);
            assertThat(transaction).isNotNull();
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        });
    }
}