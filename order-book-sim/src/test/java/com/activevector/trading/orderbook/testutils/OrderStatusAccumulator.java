package com.activevector.trading.orderbook.testutils;

import com.activevector.trading.orderbook.OrderCancelEvent;
import com.activevector.trading.orderbook.OrderFillEvent;
import com.activevector.trading.orderbook.OrderStatusListener;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.List;

public class OrderStatusAccumulator extends TestWatcher implements OrderStatusListener {
    private final List<OrderFillEvent> fills = new ArrayList<>();
    private final List<OrderCancelEvent> cancellations = new ArrayList<>();

    public void reset() {
        fills.clear();
        cancellations.clear();
    }

    @Override
    protected void starting(final Description description) {
        reset();
    }

    @Override
    public void orderFilled(final String orderId, final long fillPrice, final int fillSize, final boolean lastFill) {
        fills.add(new OrderFillEvent(orderId, fillPrice, fillSize, lastFill));
    }

    @Override
    public void orderCancelled(final String orderId) {
        cancellations.add(new OrderCancelEvent(orderId));
    }

    public List<OrderFillEvent> getFills() {
        return new ArrayList<>(fills);
    }

    public List<OrderCancelEvent> getCancellations() {
        return new ArrayList<>(cancellations);
    }
}
