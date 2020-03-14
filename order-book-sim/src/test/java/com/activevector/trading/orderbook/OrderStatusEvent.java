package com.activevector.trading.orderbook;

import java.util.Objects;

import static java.lang.String.format;

public abstract class OrderStatusEvent {
    private final String orderId;

    public OrderStatusEvent(final String orderId) {
        this.orderId = orderId;
    }

    public final String getOrderId() {
        return orderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this.getClass().isInstance(obj)) {
            final OrderStatusEvent that = (OrderStatusEvent) obj;
            return Objects.equals(this.getOrderId(), that.getOrderId());
        }
        return false;
    }

    @Override
    public String toString() {
        return format("%s(orderId=%s)", this.getClass().getSimpleName(), orderId);
    }
}
