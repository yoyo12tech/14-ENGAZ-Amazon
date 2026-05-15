package saga;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
public class SagaE2ETest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testA_should_create_transaction_when_order_completed() {

        // GIVEN
        Long orderId = 1L;
        Long userId = 1L;

        OrderCompletedEvent event =
                new OrderCompletedEvent(orderId, userId, 10L, 100.0);

        // WHEN
        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.completed",
                event
        );

        // THEN
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {

                    Transaction tx =
                            transactionRepository.findByOrderId(orderId);

                    assertNotNull(tx, "Transaction should be created by saga");
                    assertEquals(orderId, tx.getOrderId());
                    assertEquals(userId, tx.getUserId());
                });
    }
}