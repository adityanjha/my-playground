package com.activevector.trading.orderbook;

import java.util.Objects;

import static java.lang.String.format;

public class OrderFillEvent extends OrderStatusEvent {
    private final long fillPrice;
    private final int fillSize;
    private final boolean lastFill;

    public OrderFillEvent(final String orderId, final long fillPrice, final int fillSize, final boolean lastFill) {
        super(orderId);
        this.fillPrice = fillPrice;
        this.fillSize = fillSize;
        this.lastFill = lastFill;
    }

    public final long getFillPrice() {
        return fillPrice;
    }

    public final int getFillSize() {
        return fillSize;
    }

    public final boolean isLastFill() {
        return lastFill;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrderId(), fillPrice, fillSize, lastFill);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof OrderFillEvent) {
            final OrderFillEvent that = (OrderFillEvent) obj;
            return Objects.equals(this.getOrderId(), that.getOrderId())
                    && this.fillPrice == that.fillPrice
                    && this.fillSize == that.fillSize
                    && this.lastFill == that.lastFill;
        }
        return false;
    }

    @Override
    public String toString() {
        return format("%s(orderId=%s, fillPrice=%.2f fillSize=%d, lastFill=%s)",
                this.getClass().getSimpleName(), getOrderId(), fillPrice / 100D, fillSize, lastFill);
    }
}
