package com.team14.amazon.order.service;

import com.team14.amazon.contracts.dto.*;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.repository.OrderItemRepository;
import com.team14.amazon.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class OrderReadService {

    private static final Logger log = LoggerFactory.getLogger(OrderReadService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderReadService(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ── GET /api/orders/user/{userId}/summary ────────────────────────────────
    public OrderSummaryDTO getUserOrderSummary(Long userId) {
        MDC.put("userId", userId.toString());
        try {
            log.info("Computing order summary for userId={}", userId);
            Map<String, Object> raw = orderRepository.getUserOrderSummaryRaw(userId);

            long total     = toLong(raw.get("total_orders"));
            long completed = toLong(raw.get("completed_orders"));
            long cancelled = toLong(raw.get("cancelled_orders"));
            double spent   = toDouble(raw.get("total_spent"));
            double avg     = total > 0 ? spent / completed : 0.0;

            log.info("Order summary for userId={}: total={}, completed={}, cancelled={}, spent={}",
                    userId, total, completed, cancelled, spent);
            return new OrderSummaryDTO(total, completed, cancelled, spent, avg);
        } finally {
            MDC.remove("userId");
        }
    }

    // ── GET /api/orders/user/{userId}/active-count ───────────────────────────
    public int getActiveOrderCount(Long userId) {
        MDC.put("userId", userId.toString());
        try {
            int count = orderRepository.getActiveOrderCountForUser(userId);
            log.info("Active order count for userId={}: {}", userId, count);
            return count;
        } finally {
            MDC.remove("userId");
        }
    }

    // ── GET /api/orders/user/{userId}/count ──────────────────────────────────
    public long getTotalOrderCount(Long userId) {
        MDC.put("userId", userId.toString());
        try {
            long count = orderRepository.getDeliveredOrderCountForUser(userId);
            log.info("Delivered order count for userId={}: {}", userId, count);
            return count;
        } finally {
            MDC.remove("userId");
        }
    }

    // ── GET /api/orders/product/{productId}/sales ────────────────────────────
    public ProductSalesAggregateDTO getProductSales(Long productId,
                                                    String startDate,
                                                    String endDate) {
        MDC.put("productId", productId.toString());
        try {
            log.info("Computing product sales for productId={} [{} – {}]",
                    productId, startDate, endDate);
            Map<String, Object> raw =
                    orderRepository.getProductSalesRaw(productId, startDate, endDate);

            long units   = toLong(raw.get("total_units_sold"));
            double rev   = toDouble(raw.get("total_revenue"));
            double avg   = toDouble(raw.get("avg_selling_price"));

            log.info("Product sales for productId={}: units={}, revenue={}", productId, units, rev);
            return new ProductSalesAggregateDTO(units, rev, avg);
        } finally {
            MDC.remove("productId");
        }
    }

    // ── GET /api/orders/product/{productId}/pending-count ────────────────────
    public int getPendingOrderCountForProduct(Long productId) {
        MDC.put("productId", productId.toString());
        try {
            int count = orderRepository.getPendingOrderCountForProduct(productId);
            log.info("Pending order count for productId={}: {}", productId, count);
            return count;
        } finally {
            MDC.remove("productId");
        }
    }

    // ── GET /api/orders/product/{productId}/units-sold ───────────────────────
    public long getUnitsSold(Long productId) {
        MDC.put("productId", productId.toString());
        try {
            long units = orderRepository.getUnitsSoldForProduct(productId);
            log.info("Units sold for productId={}: {}", productId, units);
            return units;
        } finally {
            MDC.remove("productId");
        }
    }

    // ── GET /api/orders/product/{productId}/recent-sales-count ───────────────
    public int getRecentSalesCount(Long productId, int days) {
        MDC.put("productId", productId.toString());
        try {
            int count = orderRepository.getRecentSalesCountForProduct(productId, days);
            log.info("Recent sales ({}d) for productId={}: {}", days, productId, count);
            return count;
        } finally {
            MDC.remove("productId");
        }
    }

    // ── GET /api/orders/user/{userId}/has-purchased/{productId} ──────────────
    public boolean hasUserPurchasedProduct(Long userId, Long productId) {
        MDC.put("userId", userId.toString());
        MDC.put("productId", productId.toString());
        try {
            boolean result = orderRepository.hasUserPurchasedProduct(userId, productId);
            log.info("hasUserPurchasedProduct userId={} productId={}: {}", userId, productId, result);
            return result;
        } finally {
            MDC.remove("userId");
            MDC.remove("productId");
        }
    }

    // ── GET /api/orders/{orderId}/items ───────────────────────────────────────
    public List<OrderItemDTO> getOrderItems(Long orderId) {
        MDC.put("orderId", orderId.toString());
        try {
            // Verify the order exists first
            orderRepository.findById(orderId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Order not found: " + orderId));

            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            log.info("Returning {} items for orderId={}", items.size(), orderId);
            return items.stream()
                    .map(i -> new OrderItemDTO(
                            i.getId(),
                            i.getProductId(),
                            i.getQuantity(),
                            i.getPriceAtPurchase(),
                            i.getItemOrder()))
                    .toList();
        } finally {
            MDC.remove("orderId");
        }
    }

    // ── type coercion helpers (native SQL returns Object) ────────────────────
    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }
}