package com.activevector.trading.orderbook;

public class OrderCancelEvent extends OrderStatusEvent {
    public OrderCancelEvent(final String orderId) {
        super(orderId);
    }
}
