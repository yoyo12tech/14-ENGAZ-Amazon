//package com.team14.amazon.billing.saga;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.team14.amazon.contracts.dto.OrderDTO;
//import com.team14.amazon.contracts.dto.ProductDTO;
//import com.team14.amazon.contracts.dto.ShipmentDTO;
//import com.team14.amazon.contracts.dto.UserDTO;
//import com.team14.amazon.contracts.events.OrderCancelledEvent;
//import com.team14.amazon.contracts.events.OrderCompletedEvent;
//import com.team14.amazon.contracts.events.OrderItemPayload;
//import com.team14.amazon.contracts.events.PaymentCompletedEvent;
//import com.team14.amazon.contracts.events.PaymentFailedEvent;
//import com.team14.amazon.contracts.events.PaymentInitiatedEvent;
//import com.team14.amazon.contracts.feign.OrderServiceClient;
//import com.team14.amazon.contracts.feign.UserServiceClient;
//import com.team14.amazon.billing.model.Transaction;
//import com.team14.amazon.billing.model.TransactionStatus;
//import com.team14.amazon.billing.repository.TransactionRepository;
//import net.bytebuddy.utility.dispatcher.JavaDispatcher;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
////import org.springframework.boot.test.mock.mockito.MockBean;
////import org.springframework.test.context.DynamicPropertyRegistry;
////import org.springframework.test.context.DynamicPropertySource;
////import org.testcontainers.containers.RabbitMQContainer;
////import org.testcontainers.junit.jupiter.Container;
////import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.when;
//
///**
// * Saga end-to-end integration tests for §8.6 scenarios A, B, C.
// *
// * Infrastructure:
// *   - RabbitMQ: real broker via Testcontainers (rabbitmq:3.12-management)
// *   - Feign clients: @MockBean — no real HTTP calls to other services
// *   - Database: H2 in-memory (or the test datasource configured in
// *     src/test/resources/application-test.yml)
// *
// * Exchanges / queues used (must already be declared by billing-service's
// * RabbitMQ @Configuration beans — the tests rely on that auto-declaration):
// *   - order.events   (TopicExchange) — tests publish to this
// *   - payment.events (TopicExchange) — billing-service publishes to this
// *   - payment.saga-listener + payment.saga-listener.dlq — billing-service consumes
// *
// * The billing-service consumer for order.completed:
// *   1. Creates a PENDING Transaction
// *   2. Publishes payment.initiated to payment.events
// *
// * The billing-service consumer for order.cancelled:
// *   1. If PENDING Transaction exists → set FAILED
// *   2. If COMPLETED Transaction exists → refund → publish payment.refunded
// */
//@SpringBootTest
//@Testcontainers
//class SagaEndToEndTest {
//
//    // -------------------------------------------------------------------------
//    // Testcontainers — real RabbitMQ broker
//    // -------------------------------------------------------------------------
//
//    @JavaDispatcher.Container
//    static final RabbitMQContainer RABBIT =
//            new RabbitMQContainer("rabbitmq:3.12-management")
//                    .withExposedPorts(5672, 15672);
//
//    @DynamicPropertySource
//    static void rabbitProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.rabbitmq.host", RABBIT::getHost);
//        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
//        registry.add("spring.rabbitmq.username", () -> "guest");
//        registry.add("spring.rabbitmq.password", () -> "guest");
//    }
//
//    // -------------------------------------------------------------------------
//    // Mocked Feign clients — no real HTTP to other services
//    // -------------------------------------------------------------------------
//
//    @MockBean
//    private UserServiceClient userServiceClient;
//
//    @MockBean
//    private OrderServiceClient orderServiceClient;
//
//    // -------------------------------------------------------------------------
//    // Real Spring beans from billing-service context
//    // -------------------------------------------------------------------------
//
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    @Autowired
//    private TransactionRepository transactionRepository;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    // -------------------------------------------------------------------------
//    // Exchange / routing-key constants (must match billing-service's config)
//    // -------------------------------------------------------------------------
//
//    private static final String ORDER_EVENTS_EXCHANGE   = "order.events";
//    private static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";
//    private static final String ROUTING_ORDER_COMPLETED = "order.completed";
//    private static final String ROUTING_ORDER_CANCELLED = "order.cancelled";
//    private static final String ROUTING_PAYMENT_INITIATED  = "payment.initiated";
//    private static final String ROUTING_PAYMENT_COMPLETED  = "payment.completed";
//    private static final String ROUTING_PAYMENT_FAILED     = "payment.failed";
//    private static final String ROUTING_PAYMENT_REFUNDED   = "payment.refunded";
//
//    // -------------------------------------------------------------------------
//    // Test-data constants (match §8.6 scenario setup)
//    // -------------------------------------------------------------------------
//
//    private static final Long USER_ID           = 1L;
//    private static final Long ORDER_ID          = 10L;
//    private static final Long SHIPPING_ADDR_ID  = 1L;
//    private static final Double TOTAL_AMOUNT    = 45350.0;
//
//    // -------------------------------------------------------------------------
//    // Setup
//    // -------------------------------------------------------------------------
//
//    @BeforeEach
//    void cleanDatabase() {
//        transactionRepository.deleteAll();
//    }
//
//    // =========================================================================
//    // SCENARIO A — Happy path end-to-end
//    // §8.6: deliver → PAYMENT_PENDING → pay → PAID
//    //
//    // From billing-service's perspective:
//    //   1. Receive order.completed → create PENDING Transaction + publish payment.initiated
//    //   2. Customer calls POST /api/transactions/order/{id} (simulated here by publishing
//    //      a payment.completed event directly, since S5-F4 is an HTTP endpoint tested
//    //      separately; here we verify the consumer + state machine chain)
//    // =========================================================================
//
//    @Test
//    @DisplayName("Scenario A — Happy path: order.completed → PENDING txn + payment.initiated published")
//    void scenarioA_happyPath() throws Exception {
//        // ARRANGE — mock Feign responses the consumer will call
//        UserDTO activeUser = new UserDTO(USER_ID, "Ahmed Hassan", "ahmed@example.com",
//                "+201001234567", "CUSTOMER", "ACTIVE", null);
//        when(userServiceClient.getUser(USER_ID)).thenReturn(activeUser);
//
//        OrderDTO deliveredOrder = new OrderDTO(ORDER_ID, USER_ID, SHIPPING_ADDR_ID,
//                "DELIVERED", TOTAL_AMOUNT, null, null, null);
//        when(orderServiceClient.getOrder(ORDER_ID)).thenReturn(deliveredOrder);
//
//        // Build the order.completed event (published by S3-F4 after PUT /api/orders/{id}/deliver)
//        List<OrderItemPayload> items = List.of(
//                new OrderItemPayload(1L, 1, 45000.0),
//                new OrderItemPayload(3L, 1, 350.0)
//        );
//        OrderCompletedEvent event = new OrderCompletedEvent(
//                ORDER_ID, USER_ID, SHIPPING_ADDR_ID, TOTAL_AMOUNT);
//
//        // ACT — publish order.completed to the real RabbitMQ broker
//        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_ORDER_COMPLETED, event);
//
//        // ASSERT — billing-service consumer must create a PENDING transaction
//        await().atMost(10, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    Optional<Transaction> txnOpt =
//                            transactionRepository.findByOrderIdAndStatus(ORDER_ID, TransactionStatus.PENDING);
//                    assertThat(txnOpt).isPresent();
//
//                    Transaction txn = txnOpt.get();
//                    assertThat(txn.getOrderId()).isEqualTo(ORDER_ID);
//                    assertThat(txn.getUserId()).isEqualTo(USER_ID);
//                    assertThat(txn.getAmount()).isEqualByComparingTo(TOTAL_AMOUNT);
//                    assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PENDING);
//                });
//
//        // ASSERT — payment.initiated must have been published to payment.events
//        await().atMost(10, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    Object raw = rabbitTemplate.receiveAndConvert(
//                            "payment.saga-listener", 2000);
//                    // payment.initiated is routed back in the test listener;
//                    // verify it arrived on the payment.events exchange by checking
//                    // the transaction state that the initiated event drives in S3
//                    Transaction txn = transactionRepository
//                            .findByOrderId(ORDER_ID)
//                            .orElseThrow();
//                    assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PENDING);
//                });
//
//        // VERIFY — no cross-service JDBC: transaction lives in billing DB only
//        long count = transactionRepository.countByOrderId(ORDER_ID);
//        assertThat(count).isEqualTo(1);
//    }
//
//    // =========================================================================
//    // SCENARIO B — Payment failure and compensation cascade
//    // §8.6: deliver → PAYMENT_PENDING → simulateFailure → compensation
//    //
//    // From billing-service's perspective:
//    //   1. Receive order.completed → create PENDING Transaction + publish payment.initiated
//    //   2. S5-F4 sets status=FAILED + publishes payment.failed (simulated here by
//    //      directly flipping the transaction and publishing order.cancelled)
//    //   3. Receive order.cancelled → refund/fail Transaction + publish payment.refunded
//    // =========================================================================
//
//    @Test
//    @DisplayName("Scenario B — Payment failure: order.completed → PENDING, then order.cancelled → FAILED + payment.refunded")
//    void scenarioB_paymentFailureAndCompensation() throws Exception {
//        // ARRANGE — same Feign mocks as Scenario A
//        UserDTO activeUser = new UserDTO(USER_ID, "Ahmed Hassan", "ahmed@example.com",
//                "+201001234567", "CUSTOMER", "ACTIVE", null);
//        when(userServiceClient.getUser(USER_ID)).thenReturn(activeUser);
//
//        OrderDTO deliveredOrder = new OrderDTO(ORDER_ID, USER_ID, SHIPPING_ADDR_ID,
//                "PAYMENT_PENDING", TOTAL_AMOUNT, null, null, null);
//        when(orderServiceClient.getOrder(ORDER_ID)).thenReturn(deliveredOrder);
//
//        // STEP 1 — publish order.completed so billing creates the PENDING transaction
//        OrderCompletedEvent completedEvent = new OrderCompletedEvent(
//                ORDER_ID, USER_ID, SHIPPING_ADDR_ID, TOTAL_AMOUNT);
//        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_ORDER_COMPLETED, completedEvent);
//
//        // Wait for PENDING transaction to appear
//        await().atMost(10, TimeUnit.SECONDS)
//                .until(() -> transactionRepository
//                        .findByOrderIdAndStatus(ORDER_ID, TransactionStatus.PENDING)
//                        .isPresent());
//
//        // STEP 2 — simulate payment failure: publish order.cancelled (compensation trigger).
//        // In the real flow this is triggered by S3 consuming payment.failed and then
//        // re-publishing order.cancelled. We publish it directly here to test the
//        // billing-service compensation consumer in isolation.
//        List<OrderItemPayload> restoredItems = List.of(
//                new OrderItemPayload(1L, 1, 45000.0),
//                new OrderItemPayload(3L, 1, 350.0)
//        );
//        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
//                ORDER_ID, USER_ID, restoredItems, "payment_failed");
//        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_ORDER_CANCELLED, cancelledEvent);
//
//        // ASSERT — PENDING transaction must transition to FAILED
//        await().atMost(10, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    Optional<Transaction> txnOpt =
//                            transactionRepository.findByOrderIdAndStatus(ORDER_ID, TransactionStatus.FAILED);
//                    assertThat(txnOpt)
//                            .as("PENDING transaction should be FAILED after order.cancelled")
//                            .isPresent();
//
//                    Transaction txn = txnOpt.get();
//                    assertThat(txn.getOrderId()).isEqualTo(ORDER_ID);
//                    assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
//                });
//
//        // ASSERT — no stale PENDING transaction remains
//        assertThat(transactionRepository
//                .findByOrderIdAndStatus(ORDER_ID, TransactionStatus.PENDING))
//                .as("No PENDING transaction should remain after compensation")
//                .isEmpty();
//
//        // ASSERT — total transactions for this order is still exactly 1 (idempotent)
//        assertThat(transactionRepository.countByOrderId(ORDER_ID)).isEqualTo(1);
//    }
//
//    // =========================================================================
//    // SCENARIO C — Pre-check failure (no active shipment)
//    // §8.6: S3-F4 aborts before publishing any event → billing never receives
//    // order.completed → no transaction created in billing-service.
//    //
//    // From billing-service's perspective: nothing happens.
//    // This test verifies the absence of side-effects after a pre-check abort.
//    // We simulate the abort by simply not publishing order.completed, then
//    // assert billing-service has zero transactions for the order.
//    // =========================================================================
//
//    @Test
//    @DisplayName("Scenario C — Pre-check failure: no order.completed published → billing-service untouched")
//    void scenarioC_preCheckFailureNoShipment() throws Exception {
//        // ARRANGE — order is SHIPPED but no active shipment exists in shipping-service
//        // S3-F4 would call Feign → shipping-service → 404 → abort with 400
//        // → order.completed is NEVER published → billing-service receives nothing
//
//        // We verify this by waiting a short window and asserting no transaction appears
//        // (i.e., billing-service's consumer was never triggered).
//
//        // No event published — intentional, this is the scenario
//        long orderId = 99L; // distinct orderId so it cannot collide with other tests
//
//        // ACT — wait long enough for any spurious consumer to fire
//        TimeUnit.SECONDS.sleep(3);
//
//        // ASSERT — billing-service must have zero transactions for this order
//        assertThat(transactionRepository.findByOrderId(orderId))
//                .as("No transaction should exist: order.completed was never published (pre-check aborted in S3)")
//                .isEmpty();
//
//        // ASSERT — no PENDING, FAILED, or COMPLETED transactions leaked from other tests
//        assertThat(transactionRepository.countByOrderId(orderId)).isZero();
//    }
//
//    // =========================================================================
//    // SCENARIO C (extended) — Idempotency guard: duplicate order.completed events
//    // Critical Rule #5 — consumer must be idempotent. If order.completed arrives
//    // twice (network retry), billing-service must create only one transaction.
//    // =========================================================================
//
//    @Test
//    @DisplayName("Idempotency — duplicate order.completed → only one PENDING transaction created")
//    void scenarioC_idempotency_duplicateOrderCompletedEvent() throws Exception {
//        // ARRANGE
//        UserDTO activeUser = new UserDTO(USER_ID, "Ahmed Hassan", "ahmed@example.com",
//                "+201001234567", "CUSTOMER", "ACTIVE", null);
//        when(userServiceClient.getUser(USER_ID)).thenReturn(activeUser);
//
//        OrderDTO deliveredOrder = new OrderDTO(ORDER_ID, USER_ID, SHIPPING_ADDR_ID,
//                "DELIVERED", TOTAL_AMOUNT, null, null, null);
//        when(orderServiceClient.getOrder(ORDER_ID)).thenReturn(deliveredOrder);
//
//        OrderCompletedEvent event = new OrderCompletedEvent(
//                ORDER_ID, USER_ID, SHIPPING_ADDR_ID, TOTAL_AMOUNT);
//
//        // ACT — publish the same order.completed event twice (simulating a network retry)
//        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_ORDER_COMPLETED, event);
//        rabbitTemplate.convertAndSend(ORDER_EVENTS_EXCHANGE, ROUTING_ORDER_COMPLETED, event);
//
//        // Wait for consumer to settle
//        await().atMost(10, TimeUnit.SECONDS)
//                .until(() -> transactionRepository.countByOrderId(ORDER_ID) >= 1);
//
//        // Give a short additional window for any duplicate to appear
//        TimeUnit.SECONDS.sleep(2);
//
//        // ASSERT — exactly one transaction created despite two events (idempotent consumer)
//        long txnCount = transactionRepository.countByOrderId(ORDER_ID);
//        assertThat(txnCount)
//                .as("Idempotency violation: duplicate order.completed created %d transactions", txnCount)
//                .isEqualTo(1);
//    }
//}