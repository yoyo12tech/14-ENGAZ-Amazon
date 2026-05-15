package com.team14.amazon.billing.messaging;

import com.team14.amazon.billing.model.Transaction;
import com.team14.amazon.billing.model.TransactionMethod;
import com.team14.amazon.billing.model.TransactionStatus;
import com.team14.amazon.billing.repository.TransactionRepository;
import com.team14.amazon.billing.service.PartialRefundService;
import com.team14.amazon.billing.dto.RefundRequest;
import com.team14.amazon.contracts.dto.UserDTO;
import com.team14.amazon.contracts.events.*;
import com.team14.amazon.contracts.feign.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PartialRefundService partialRefundService;

    public OrderEventConsumer(TransactionRepository transactionRepository,
                              UserServiceClient userServiceClient,
                              PaymentEventPublisher paymentEventPublisher,
                              PartialRefundService partialRefundService) {
        this.transactionRepository = transactionRepository;
        this.userServiceClient = userServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.partialRefundService = partialRefundService;
    }

    @RabbitListener(queues = "payment.saga-listener")
    public void onOrderEvent(Object event) {
        if (event instanceof OrderCompletedEvent completedEvent) {
            handleOrderCompleted(completedEvent);
        } else if (event instanceof OrderCancelledEvent cancelledEvent) {
            handleOrderCancelled(cancelledEvent);
        }
    }

    private void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received OrderCompletedEvent for order {}", event.orderId());

        // Fix 1 (Idempotency)
        if (transactionRepository.findByOrderId(event.orderId()) != null) {
            log.info("Transaction for order {} already exists. Skipping OrderCompletedEvent.", event.orderId());
            return;
        }

        UserDTO user = userServiceClient.getUser(event.userId());
        if (user == null) {
            log.error("User {} not found", event.userId());
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(event.orderId());
        transaction.setUserId(event.userId());
        transaction.setAmount(event.totalAmount());
        transaction.setMethod(TransactionMethod.CREDIT_CARD); // Default or based on logic
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);

        paymentEventPublisher.publishInitiated(new PaymentInitiatedEvent(
                transaction.getId(), event.orderId(), event.userId(), event.totalAmount()));
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order {}", event.orderId());
        Transaction transaction = transactionRepository.findByOrderId(event.orderId());
        if (transaction != null) {
            if (transaction.getStatus() == TransactionStatus.COMPLETED) {
                // Fix 2 (Full Refund Logic)
                RefundRequest request = new RefundRequest();
                request.setReason(event.reason());
                request.setRefundAll(true);

                try {
                    partialRefundService.processPartialRefund(transaction.getId(), request);
                } catch (Exception e) {
                    log.error("Failed to process refund for transaction {}", transaction.getId(), e);
                }
            } else if (transaction.getStatus() == TransactionStatus.PENDING) {
                int updated = transactionRepository.updateStatusAtomically(
                        transaction.getId(), TransactionStatus.PENDING, TransactionStatus.FAILED);
                if (updated > 0) {
                    paymentEventPublisher.publishFailed(new PaymentFailedEvent(
                            transaction.getId(), event.orderId(), event.reason()));
                }
            }
        }
    }
}
