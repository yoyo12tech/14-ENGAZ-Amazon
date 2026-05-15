package saga;
import com.team14.amazon.billing.BillingServiceApplication;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.contracts.events.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = BillingServiceApplication.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        }
)
class SagaE2ETest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testA_happy_path_saga_flow() {

        Long orderId = 10L;
        Long userId = 1L;

        // ---------------------------
        // STEP 1: ORDER COMPLETED
        // ---------------------------
        OrderCompletedEvent orderEvent =
                new OrderCompletedEvent(orderId, userId, 1L, 45350.0);

        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.completed",
                orderEvent
        );

        // ---------------------------
        // VERIFY: PENDING transaction created
        // ---------------------------
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {

                    Transaction tx =
                            transactionRepository.findByOrderId(orderId);

                    assertNotNull(tx);
                    assertEquals(orderId, tx.getOrderId());
                    assertEquals(userId, tx.getUserId());
                    assertEquals(TransactionStatus.PENDING, tx.getStatus());

                    assertEquals(
                            new BigDecimal("45350"),
                            tx.getAmount()
                    );
                });

        // ---------------------------
        // STEP 2: PAYMENT COMPLETED
        // ---------------------------
        PaymentCompletedEvent paymentEvent =
                new PaymentCompletedEvent(
                        999L,        // transactionId (can be dummy if not enforced)
                        orderId,
                        userId,
                        45350.0
                );

        rabbitTemplate.convertAndSend(
                "payment.exchange",
                "payment.completed",
                paymentEvent
        );

        // ---------------------------
        // VERIFY: transaction completed
        // ---------------------------
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {

                    Transaction tx =
                            transactionRepository.findByOrderId(orderId);

                    assertEquals(TransactionStatus.COMPLETED, tx.getStatus());
                });
    }
}