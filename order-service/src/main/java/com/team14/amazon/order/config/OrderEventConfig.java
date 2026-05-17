package com.team14.amazon.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 * M3 RabbitMQ topology for order-service.
 *
 * Named OrderEventConfig per spec §12 folder tree.
 * This REPLACES the old RabbitMQConfig.java entirely.
 *
 * Spec §2.7 topology rules:
 *   Producer declares ONLY its own TopicExchange.
 *   Consumer declares Queue + DLQ + reference exchange + Binding.
 *
 * order-service PUBLISHES to  : order.events        (this service owns it)
 * order-service CONSUMES from : shipment.events, payment.events
 *
 * Single consumer queue: order.saga-feedback  (spec §5 / §2.7)
 * DLQ:                   order.saga-feedback.dlq
 * DLX (internal):        order.saga-feedback.dlx
 */
@Configuration
public class OrderEventConfig {

    // ── Exchange names (used by publishers and consumers) ─────────────────────
    public static final String ORDER_EXCHANGE    = "order.events";
    public static final String SHIPMENT_EXCHANGE = "shipment.events";
    public static final String PAYMENT_EXCHANGE  = "payment.events";

    // ── Queue / DLQ names ─────────────────────────────────────────────────────
    public static final String SAGA_FEEDBACK_QUEUE = "order.saga-feedback";
    public static final String SAGA_FEEDBACK_DLQ   = "order.saga-feedback.dlq";
    private static final String SAGA_FEEDBACK_DLX  = "order.saga-feedback.dlx";

    // ── Routing keys order-service PUBLISHES ──────────────────────────────────
    public static final String ROUTING_ORDER_PLACED    = "order.placed";
    public static final String ROUTING_ORDER_COMPLETED = "order.completed";
    public static final String ROUTING_ORDER_CANCELLED = "order.cancelled";

    // ── Routing keys order-service CONSUMES ───────────────────────────────────
    public static final String ROUTING_SHIPMENT_CREATED        = "shipment.created";
    public static final String ROUTING_SHIPMENT_STATUS_CHANGED = "shipment.status-changed";
    public static final String ROUTING_PAYMENT_INITIATED       = "payment.initiated";
    public static final String ROUTING_PAYMENT_COMPLETED       = "payment.completed";
    public static final String ROUTING_PAYMENT_FAILED          = "payment.failed";
    public static final String ROUTING_PAYMENT_REFUNDED        = "payment.refunded";

    // =========================================================================
    // JSON message converter — required for Java record deserialization
    // =========================================================================

    @Bean
    @Primary
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Override listener factory: inject JSON converter + enforce auto-ACK
     * with no-requeue so failed messages go to DLQ after retries (Critical Rule #3).
     */
    @Bean
    @Primary
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // =========================================================================
    // Exchanges
    // =========================================================================

    /** order-service OWNS this exchange — publishes order.placed/completed/cancelled */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * Reference declaration for shipment.events.
     * shipping-service owns/creates it; Spring deduplicates on the broker.
     */
    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange(SHIPMENT_EXCHANGE, true, false);
    }

    /**
     * Reference declaration for payment.events.
     * billing-service owns/creates it; Spring deduplicates on the broker.
     */
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    /** Internal dead-letter exchange — DirectExchange for precise DLQ routing */
    @Bean
    public DirectExchange sagaFeedbackDlx() {
        return new DirectExchange(SAGA_FEEDBACK_DLX, true, false);
    }

    // =========================================================================
    // Consumer queue + DLQ
    // =========================================================================

    /**
     * Single consumer queue for ALL saga feedback (shipment + payment).
     * On rejection after retries → routed to DLX → DLQ automatically.
     * Critical Rule #4: every consumer queue has a DLQ.
     */
    @Bean
    public Queue sagaFeedbackQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SAGA_FEEDBACK_DLX);
        args.put("x-dead-letter-routing-key", SAGA_FEEDBACK_DLQ);
        return new Queue(SAGA_FEEDBACK_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue sagaFeedbackDlq() {
        return new Queue(SAGA_FEEDBACK_DLQ, true, false, false);
    }

    @Bean
    public Binding dlqBinding(Queue sagaFeedbackDlq, DirectExchange sagaFeedbackDlx) {
        return BindingBuilder.bind(sagaFeedbackDlq)
                .to(sagaFeedbackDlx)
                .with(SAGA_FEEDBACK_DLQ);
    }

    // =========================================================================
    // Bindings: route shipment events → order.saga-feedback
    // =========================================================================

    @Bean
    public Binding bindShipmentCreated(Queue sagaFeedbackQueue,
                                       TopicExchange shipmentExchange) {
        return BindingBuilder.bind(sagaFeedbackQueue)
                .to(shipmentExchange)
                .with(ROUTING_SHIPMENT_CREATED);
    }

    @Bean
    public Binding bindShipmentStatusChanged(Queue sagaFeedbackQueue,
                                             TopicExchange shipmentExchange) {
        return BindingBuilder.bind(sagaFeedbackQueue)
                .to(shipmentExchange)
                .with(ROUTING_SHIPMENT_STATUS_CHANGED);
    }

    // =========================================================================
    // Bindings: route payment events → order.saga-feedback
    // =========================================================================

    @Bean
    public Binding bindPaymentInitiated(Queue sagaFeedbackQueue,
                                        TopicExchange paymentExchange) {
        return BindingBuilder.bind(sagaFeedbackQueue)
                .to(paymentExchange)
                .with(ROUTING_PAYMENT_INITIATED);
    }

    @Bean
    public Binding bindPaymentCompleted(Queue sagaFeedbackQueue,
                                        TopicExchange paymentExchange) {
        return BindingBuilder.bind(sagaFeedbackQueue)
                .to(paymentExchange)
                .with(ROUTING_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding bindPaymentFailed(Queue sagaFeedbackQueue,
                                     TopicExchange paymentExchange) {
        return BindingBuilder.bind(sagaFeedbackQueue)
                .to(paymentExchange)
                .with(ROUTING_PAYMENT_FAILED);
    }

    @Bean
    public Binding bindPaymentRefunded(Queue sagaFeedbackQueue,
                                       TopicExchange paymentExchange) {
        return BindingBuilder.bind(sagaFeedbackQueue)
                .to(paymentExchange)
                .with(ROUTING_PAYMENT_REFUNDED);
    }
}