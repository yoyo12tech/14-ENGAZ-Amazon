package com.team14.amazon.order.service;

import com.team14.amazon.order.config.CacheEvictionService;
import com.team14.amazon.order.model.Order;
import com.team14.amazon.order.model.OrderItem;
import com.team14.amazon.order.observer.EntityObserver;
import com.team14.amazon.order.repository.OrderItemRepository;
import com.team14.amazon.order.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * M2 retrofit of OrderItemService:
 * DP-2 Observer : registers MongoEventLogger in @PostConstruct.
 * CC-3 Caching  : @Cacheable on getById; CacheEvictionService on writes.
 */
@Service
public class OrderItemService {

    private final OrderItemRepository  orderItemRepository;
    private final OrderRepository      orderRepository;
    private final CacheEvictionService cacheEvictionService;
    private final EntityObserver       mongoEventLogger;

    private final List<EntityObserver> observers = new ArrayList<>();

    public OrderItemService(OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository,
                            CacheEvictionService cacheEvictionService,
                            EntityObserver mongoEventLogger) {
        this.orderItemRepository  = orderItemRepository;
        this.orderRepository      = orderRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.mongoEventLogger     = mongoEventLogger;
    }

    @PostConstruct
    public void init() {
        register(mongoEventLogger);
    }

    public void register(EntityObserver observer)   { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    public OrderItem create(Long orderId, OrderItem item) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        Map<String, Object> product = orderRepository.findProductById(item.getProductId());
        if (product == null || product.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product not found: " + item.getProductId());
        }
        item.setOrder(order);
        item.setPriceAtPurchase(((Number) product.get("price")).doubleValue());
        OrderItem saved = orderItemRepository.save(item);

        cacheEvictionService.evictOrderFeatureCaches();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId",     orderId);
        payload.put("orderItemId", saved.getId());
        notifyObservers("ORDER_ITEM_CREATED", payload);
        return saved;
    }

    @Cacheable(value = "order-service::order-item", key = "#id")
    public Optional<OrderItem> getById(Long id) {
        return orderItemRepository.findById(id);
    }

    public OrderItem getOrderItem(Long orderId, Long itemId) {
        return orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order item not found"));
    }

    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }

    @Transactional
    public OrderItem update(Long id, OrderItem updated) {
        OrderItem existing = orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrderItem not found: " + id));

        if (updated.getQuantity()       != null) existing.setQuantity(updated.getQuantity());
        if (updated.getMetadata()       != null) existing.setMetadata(updated.getMetadata());
        if (updated.getItemOrder()      != null) existing.setItemOrder(updated.getItemOrder());
        if (updated.getPriceAtPurchase() != null) existing.setPriceAtPurchase(updated.getPriceAtPurchase());

        OrderItem saved = orderItemRepository.save(existing);
        cacheEvictionService.evictOrderItemDetail(id);
        cacheEvictionService.evictOrderFeatureCaches();
        return saved;
    }

    public void delete(Long id) {
        orderItemRepository.deleteById(id);
        cacheEvictionService.evictOrderItemDetail(id);
    }

    public void deleteOrderItem(Long orderId, Long itemId) {
        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order item not found"));
        orderItemRepository.delete(item);
        cacheEvictionService.evictOrderItemDetail(itemId);
    }

    public int findMaxItemOrderByOrderId2(Long orderId) {
        return orderItemRepository.findMaxItemOrderByOrderId(orderId);
    }
}