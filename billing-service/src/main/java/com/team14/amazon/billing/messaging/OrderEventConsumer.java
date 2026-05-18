package com.team14.amazon.billing.messaging;

import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.service.PartialRefundService;
import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.contracts.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "payment.saga-listener")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PartialRefundService partialRefundService;

    public OrderEventConsumer(TransactionRepository transactionRepository,
            PaymentEventPublisher paymentEventPublisher,
            PartialRefundService partialRefundService) {
        this.transactionRepository = transactionRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.partialRefundService = partialRefundService;
    }

    @RabbitHandler
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received OrderCompletedEvent for orderId={}, userId={}", event.orderId(), event.userId());

        if (transactionRepository.findByOrderId(event.orderId()) != null) {
            log.info("Transaction for order {} already exists. Skipping.", event.orderId());
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(event.orderId());
        transaction.setUserId(event.userId());
        transaction.setAmount(event.totalAmount());
        transaction.setMethod(TransactionMethod.CREDIT_CARD);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);

        log.info("Created PENDING transaction with ID={}", transaction.getId());

        paymentEventPublisher.publishInitiated(new PaymentInitiatedEvent(
                transaction.getId(), event.orderId(), event.userId(), event.totalAmount()));
    }

    @RabbitHandler
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for orderId={}", event.orderId());

        Transaction transaction = transactionRepository.findByOrderId(event.orderId());
        if (transaction != null) {
            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                int updated = transactionRepository.updateStatusAtomically(
                        transaction.getId(), TransactionStatus.COMPLETED, TransactionStatus.REFUNDED);

                if (updated > 0) {
                    log.info("Transaction {} saved with status=REFUNDED", transaction.getId());
                    paymentEventPublisher.publishRefunded(new PaymentRefundedEvent(
                            transaction.getId(), event.orderId(), transaction.getAmount()));
                    log.info("Published payment.refunded for orderId={}", event.orderId());
                }
            } else if (transaction.getStatus() == TransactionStatus.PENDING) {
                int updated = transactionRepository.updateStatusAtomically(
                        transaction.getId(), TransactionStatus.PENDING, TransactionStatus.FAILED);
                if (updated > 0) {
                    log.info("Transaction {} saved with status=FAILED", transaction.getId());
                    paymentEventPublisher.publishFailed(new PaymentFailedEvent(
                            transaction.getId(), event.orderId(), event.reason()));
                    log.info("Published payment.failed for orderId={}", event.orderId());
                }
            }
        } else {
            log.warn("Received cancel event for orderId={}, but no transaction found.", event.orderId());
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object event) {
        log.warn("Received an unrecognized event type on payment.saga-listener: {}", event.getClass().getName());
    }
}
