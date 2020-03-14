package com.activevector.trading.orderbook.model;

import com.activevector.trading.orderbook.OrderStatusListener;

import java.util.*;

import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class PriceLevel {
    private final OrderStatusListener listener;

    private final Comparator<Map.Entry<String, Integer>> comparator;
    private final Map<String, Integer> qtyByOrderId;

    private int totalQty;

    public PriceLevel(final OrderStatusListener listener, final PriceLevelFillStrategy fillStrategy) {
        this.listener = listener;

        switch (requireNonNull(fillStrategy, "fillStrategy")) {
            case FILL_IN_SEQ:
                comparator = null;
                qtyByOrderId = new LinkedHashMap<>();
                break;
            case LOWEST_QTY_FIRST:
                comparator = comparingInt(Map.Entry::getValue);
                qtyByOrderId = new HashMap<>();
                break;
            case HIGHEST_QTY_FIRST:
                comparator = comparingInt(Map.Entry<String, Integer>::getValue).reversed();
                qtyByOrderId = new HashMap<>();
                break;
            default:
                throw new IllegalArgumentException("Unknown price-level fill strategy: " + fillStrategy);
        }

        totalQty = 0;
    }

    public PriceLevel add(final String orderId, final int qty) {
        if (qtyByOrderId.containsKey(orderId)) throw new IllegalArgumentException("Order Id already exists: " + orderId);

        qtyByOrderId.put(orderId, qty);
        totalQty += qty;

        return this;
    }

    public final int getOrderCount() {
        return qtyByOrderId.size();
    }

    public final int getTotalQty() {
        return totalQty;
    }

    public int provideFill(final int size, final long fillPrice) {
        final int fill;
        if (totalQty <= size) {
            fill = totalQty;

            if (listener != null) {
                getFillOrder().forEach(e -> listener.orderFilled(e.getKey(), fillPrice, e.getValue(), true));
            }
            qtyByOrderId.clear();
        }
        else {
            extractFill(size, fillPrice, getFillOrder());
            fill = size;
        }

        totalQty -= fill;

        return fill;
    }

    private List<Map.Entry<String, Integer>> getFillOrder() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(qtyByOrderId.entrySet());
        if (comparator != null) {
            list.sort(comparator);
        }
        return list;
    }

    private void extractFill(final int size, final long fillPrice, final List<Map.Entry<String, Integer>> list) {
        int remainingFill = size;
        for (final Map.Entry<String, Integer> entry : list) {
            final String orderId = entry.getKey();
            final int orderQty = entry.getValue();

            final int fillQty;
            if (orderQty <= remainingFill) {
                qtyByOrderId.remove(orderId);
                fillQty = orderQty;
            }
            else {
                qtyByOrderId.put(orderId, orderQty - remainingFill);
                fillQty = remainingFill;
            }

            remainingFill -= fillQty;

            if (listener != null) {
                listener.orderFilled(orderId, fillPrice, fillQty, fillQty == orderQty);
            }

            if (remainingFill == 0) break;
        }
    }

    @Override
    public String toString() {
        return format("%d(%s)", totalQty, getFillOrder().stream().map(e -> format("%s=%d", e.getKey(), e.getValue())).collect(joining(",")));
    }
}
