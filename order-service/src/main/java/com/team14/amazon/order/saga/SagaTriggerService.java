package com.team14.amazon.order.saga;

import com.team14.amazon.contracts.dto.UserDTO;
import com.team14.amazon.contracts.events.OrderCompletedEvent;
import com.team14.amazon.contracts.feign.ProductServiceClient;
import com.team14.amazon.contracts.feign.ShippingServiceClient;
import com.team14.amazon.contracts.feign.UserServiceClient;
import com.team14.amazon.order.config.CacheEvictionService;
import com.team14.amazon.order.messaging.OrderEventPublisher;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.order.observer.EntityObserver;
import com.team14.amazon.order.repository.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S3-F4 — Mark Order as Delivered (Saga Trigger).
 *
 * Spec §8.3: PUT /api/orders/{id}/deliver
 *
 * Lives in saga/ package per spec §12 folder tree.
 *
 * M3 changes from M1:
 *   REMOVED — orderRepository.markShipmentDelivered()  (cross-service SQL write to shipping)
 *   REMOVED — orderRepository.createTransaction()       (cross-service SQL write to billing)
 *   ADDED   — 3 Feign pre-checks (user active, products exist, active shipment)
 *   ADDED   — publish order.completed (billing creates PENDING txn, shipping marks DELIVERED)
 *
 * Pre-check order matters (spec §8.3):
 *   1. user active check     → 400 "User is no longer active"
 *   2. products exist check  → 400 "Product no longer exists in catalog"
 *   3. active shipment check → 400 "No active shipment to deliver"
 *   All three must pass BEFORE any local write or event publish.
 *
 * Idempotency: the status guard (must be SHIPPED) prevents double-trigger.
 */
@Service
public class SagaTriggerService {

    private static final Logger log = LoggerFactory.getLogger(SagaTriggerService.class);

    private final OrderRepository       orderRepository;
    private final UserServiceClient     userServiceClient;
    private final ProductServiceClient  productServiceClient;
    private final ShippingServiceClient shippingServiceClient;
    private final OrderEventPublisher   eventPublisher;
    private final CacheEvictionService  cacheEvictionService;
    private final EntityObserver        mongoEventLogger;

    public SagaTriggerService(OrderRepository orderRepository,
                              UserServiceClient userServiceClient,
                              ProductServiceClient productServiceClient,
                              ShippingServiceClient shippingServiceClient,
                              OrderEventPublisher eventPublisher,
                              CacheEvictionService cacheEvictionService,
                              EntityObserver mongoEventLogger) {
        this.orderRepository       = orderRepository;
        this.userServiceClient     = userServiceClient;
        this.productServiceClient  = productServiceClient;
        this.shippingServiceClient = shippingServiceClient;
        this.eventPublisher        = eventPublisher;
        this.cacheEvictionService  = cacheEvictionService;
        this.mongoEventLogger      = mongoEventLogger;
    }

    /**
     * Marks an order as DELIVERED and triggers the payment saga.
     * Called by OrderController PUT /api/orders/{id}/deliver.
     */
    @Transactional
    public Order markAsDelivered(Long orderId) {
        MDC.put("orderId", orderId.toString());
        try {
            // ── Step 1: local lookup ───────────────────────────────────────────
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Order not found: " + orderId));

            // ── Step 2: status guard — must be SHIPPED ─────────────────────────
            if (order.getStatus() != OrderStatus.SHIPPED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Order must be SHIPPED before delivery");
            }

            // ── Step 3: ensure totalAmount is calculated ───────────────────────
            if (order.getTotalAmount() == null) {
                double total = order.getOrderItems().stream()
                        .mapToDouble(i -> i.getQuantity() * i.getPriceAtPurchase())
                        .sum();
                order.setTotalAmount(total);
            }

            // ── Pre-check 1: user must be ACTIVE ─────────────────────────────
            log.info("Calling UserServiceClient.getUser with args=[{}]", order.getUserId());
            try {
                UserDTO user = userServiceClient.getUser(order.getUserId());
                log.info("UserServiceClient.getUser returned successfully");
                if (!"ACTIVE".equalsIgnoreCase(user.status())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "User is no longer active");
                }
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (FeignException.NotFound e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User is no longer active");
            } catch (FeignException e) {
                log.warn("Feign call to user-service failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "User service temporarily unavailable");
            }

            // ── Pre-check 2: every product must exist in catalog ──────────────
            List<OrderItem> items = order.getOrderItems();
            for (OrderItem item : items) {
                MDC.put("productId", item.getProductId().toString());
                log.info("Calling ProductServiceClient.productExists with args=[{}]",
                        item.getProductId());
                try {
                    Map<String, Boolean> exists =
                            productServiceClient.productExists(item.getProductId());
                    log.info("ProductServiceClient.productExists returned successfully");
                    if (Boolean.FALSE.equals(exists.get("exists"))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Product no longer exists in catalog: " + item.getProductId());
                    }
                } catch (ResponseStatusException rse) {
                    throw rse;
                } catch (FeignException e) {
                    log.warn("Feign call to product-service failed: {}", e.getMessage());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Product service temporarily unavailable");
                } finally {
                    MDC.remove("productId");
                }
            }

            // ── Pre-check 3: active shipment must exist ───────────────────────
            MDC.put("shipmentId", "checking");
            log.info("Calling ShippingServiceClient.getActiveShipmentForOrder with args=[{}]",
                    orderId);
            try {
                shippingServiceClient.getActiveShipmentForOrder(orderId);
                log.info("ShippingServiceClient.getActiveShipmentForOrder returned successfully");
            } catch (FeignException.NotFound e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No active shipment to deliver");
            } catch (FeignException e) {
                log.warn("Feign call to shipping-service failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Shipping service temporarily unavailable");
            } finally {
                MDC.remove("shipmentId");
            }

            // ── Step 4: local write — set DELIVERED ───────────────────────────
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            Order saved = orderRepository.save(order);
            log.info("Order {} transitioning {} → DELIVERED", orderId, previousStatus);
            log.info("Order {} saved with status=DELIVERED", orderId);

            cacheEvictionService.evictOrderDetail(orderId);
            cacheEvictionService.evictOrderFeatureCaches();

            // ── Step 5: publish order.completed (saga trigger) ─────────────────
            // billing-service consumer → creates PENDING transaction → publishes payment.initiated
            // shipping-service consumer → marks shipment DELIVERED → publishes shipment.status-changed
            // order-service consumer   → sets status = PAYMENT_PENDING on payment.initiated
            eventPublisher.publishOrderCompleted(new OrderCompletedEvent(
                    orderId,
                    order.getUserId(),
                    order.getShippingAddressId(),
                    order.getTotalAmount()));

            // ── Observer (MongoDB event log) ───────────────────────────────────
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", orderId);
            mongoEventLogger.onEvent("ORDER_DELIVERED", payload);

            return saved;

        } finally {
            MDC.remove("orderId");
        }
    }
}