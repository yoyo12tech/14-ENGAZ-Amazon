package com.team14.amazon.order.dto;

import com.team14.amazon.order.model.OrderStatus;
import com.team14.amazon.contracts.dto.OrderItemDTO;

import java.util.List;
import java.util.Map;

/**
 * DP-4 Builder Pattern — M1 DTO converted from Java record to class.
 * Java records cannot have a static inner Builder, so we convert to a class
 * while keeping record-style component accessor names for call-site compatibility.
 *
 * Used by M1 S3-F9 GET /api/orders/{id}/details.
 */
public class OrderDetailsDTO {

    private Long                  orderId;
    private Long                  userId;
    private Long                  shippingAddressId;
    private OrderStatus           status;
    private Double                totalAmount;
    private Map<String, Object>   metadata;
    private List<OrderItemDTO>    items;
    private int                   totalItems;
    private int                   totalQuantity;

    /** Required by Jackson for Redis deserialization. */
    public OrderDetailsDTO() {}

    /** Legacy canonical constructor (mirrors the old record). */
    public OrderDetailsDTO(Long orderId, Long userId, Long shippingAddressId, OrderStatus status,
                           Double totalAmount, Map<String, Object> metadata,
                           List<OrderItemDTO> items, int totalItems, int totalQuantity) {
        this.orderId           = orderId;
        this.userId            = userId;
        this.shippingAddressId = shippingAddressId;
        this.status            = status;
        this.totalAmount       = totalAmount;
        this.metadata          = metadata;
        this.items             = items;
        this.totalItems        = totalItems;
        this.totalQuantity     = totalQuantity;
    }

    // ── Record-style accessors (for existing call sites) ──────────────────────
    public Long                orderId()           { return orderId; }
    public Long                userId()            { return userId; }
    public Long                shippingAddressId() { return shippingAddressId; }
    public OrderStatus         status()            { return status; }
    public Double              totalAmount()       { return totalAmount; }
    public Map<String, Object> metadata()          { return metadata; }
    public List<OrderItemDTO>  items()             { return items; }
    public int                 totalItems()        { return totalItems; }
    public int                 totalQuantity()     { return totalQuantity; }

    // ── Standard getters (required by Jackson) ────────────────────────────────
    public Long                getOrderId()           { return orderId; }
    public Long                getUserId()            { return userId; }
    public Long                getShippingAddressId() { return shippingAddressId; }
    public OrderStatus         getStatus()            { return status; }
    public Double              getTotalAmount()       { return totalAmount; }
    public Map<String, Object> getMetadata()          { return metadata; }
    public List<OrderItemDTO>  getItems()             { return items; }
    public int                 getTotalItems()        { return totalItems; }
    public int                 getTotalQuantity()     { return totalQuantity; }

    // ── Standard setters (required by Jackson) ────────────────────────────────
    public void setOrderId(Long v)                    { this.orderId = v; }
    public void setUserId(Long v)                     { this.userId = v; }
    public void setShippingAddressId(Long v)          { this.shippingAddressId = v; }
    public void setStatus(OrderStatus v)              { this.status = v; }
    public void setTotalAmount(Double v)              { this.totalAmount = v; }
    public void setMetadata(Map<String, Object> v)    { this.metadata = v; }
    public void setItems(List<OrderItemDTO> v)        { this.items = v; }
    public void setTotalItems(int v)                  { this.totalItems = v; }
    public void setTotalQuantity(int v)               { this.totalQuantity = v; }

    // ── DP-4: Builder ─────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final OrderDetailsDTO dto = new OrderDetailsDTO();

        public Builder orderId(Long v)                  { dto.orderId = v;           return this; }
        public Builder userId(Long v)                   { dto.userId = v;            return this; }
        public Builder shippingAddressId(Long v)        { dto.shippingAddressId = v; return this; }
        public Builder status(OrderStatus v)            { dto.status = v;            return this; }
        public Builder totalAmount(Double v)            { dto.totalAmount = v;       return this; }
        public Builder metadata(Map<String, Object> v)  { dto.metadata = v;          return this; }
        public Builder items(List<OrderItemDTO> v)      { dto.items = v;             return this; }
        public Builder totalItems(int v)                { dto.totalItems = v;        return this; }
        public Builder totalQuantity(int v)             { dto.totalQuantity = v;     return this; }

        public OrderDetailsDTO build()                  { return dto; }
    }
}
