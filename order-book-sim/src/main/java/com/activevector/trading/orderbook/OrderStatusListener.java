package com.activevector.trading.orderbook;

public interface OrderStatusListener {
    void orderFilled(String orderId, long fillPrice, int fillSize, boolean lastFill);

    void orderCancelled(String orderId);
}
