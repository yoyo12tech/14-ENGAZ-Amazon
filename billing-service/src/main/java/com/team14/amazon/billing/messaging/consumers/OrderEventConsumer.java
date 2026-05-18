package com.team14.amazon.billing.messaging.consumers;

import com.team14.amazon.billing.messaging.publishers.PaymentEventPublisher;
import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.contracts.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "payment.saga-listener")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private static final String ROUTING_ORDER_COMPLETED = "order.completed";
    private static final String ROUTING_ORDER_CANCELLED = "order.cancelled";

    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public OrderEventConsumer(TransactionRepository transactionRepository,
            PaymentEventPublisher paymentEventPublisher) {
        this.transactionRepository = transactionRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @RabbitHandler
    public void handleOrderCompleted(OrderCompletedEvent event) {
        MDC.put("routingKey", ROUTING_ORDER_COMPLETED);
        MDC.put("orderId", event.orderId().toString());
        try {
            log.info("Consuming {} for {}={}", ROUTING_ORDER_COMPLETED, "order", event.orderId());

            if (transactionRepository.findByOrderId(event.orderId()) != null) {
                log.info("Transaction for order {} already exists; skipping {}", event.orderId(),
                        ROUTING_ORDER_COMPLETED);
                return;
            }

            Transaction transaction = new Transaction();
            transaction.setOrderId(event.orderId());
            transaction.setUserId(event.userId());
            transaction.setAmount(event.totalAmount());
            transaction.setMethod(TransactionMethod.CREDIT_CARD);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction = transactionRepository.save(transaction);

            log.info("Transaction {} saved with status={}", transaction.getId(), transaction.getStatus());

            paymentEventPublisher.publishInitiated(new PaymentInitiatedEvent(
                    transaction.getId(), event.orderId(), event.userId(), event.totalAmount()));

            log.info("Processed {} for {}={}", ROUTING_ORDER_COMPLETED, "order", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process {}: {}", ROUTING_ORDER_COMPLETED, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("orderId");
        }
    }

    @RabbitHandler
    public void handleOrderCancelled(OrderCancelledEvent event) {
        MDC.put("routingKey", ROUTING_ORDER_CANCELLED);
        MDC.put("orderId", event.orderId().toString());
        try {
            log.info("Consuming {} for {}={}", ROUTING_ORDER_CANCELLED, "order", event.orderId());

            Transaction transaction = transactionRepository.findByOrderId(event.orderId());
            if (transaction == null) {
                log.warn("No transaction found for orderId={} on {}", event.orderId(), ROUTING_ORDER_CANCELLED);
                return;
            }

            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                int updated = transactionRepository.updateStatusAtomically(
                        transaction.getId(), TransactionStatus.COMPLETED, TransactionStatus.REFUNDED);

                if (updated > 0) {
                    log.info("Transaction {} saved with status={}", transaction.getId(), TransactionStatus.REFUNDED);
                    paymentEventPublisher.publishRefunded(new PaymentRefundedEvent(
                            transaction.getId(), event.orderId(), transaction.getAmount()));
                }
            } else if (transaction.getStatus() == TransactionStatus.PENDING) {
                int updated = transactionRepository.updateStatusAtomically(
                        transaction.getId(), TransactionStatus.PENDING, TransactionStatus.FAILED);
                if (updated > 0) {
                    log.info("Transaction {} saved with status={}", transaction.getId(), TransactionStatus.FAILED);
                    paymentEventPublisher.publishFailed(new PaymentFailedEvent(
                            transaction.getId(), event.orderId(), event.reason()));
                }
            }

            log.info("Processed {} for {}={}", ROUTING_ORDER_CANCELLED, "order", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process {}: {}", ROUTING_ORDER_CANCELLED, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("orderId");
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object event) {
        log.warn("Unhandled event type on payment.saga-listener: {}", event.getClass().getName());
    }
}
