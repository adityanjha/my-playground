package com.activevector.trading.orderbook;

import com.activevector.trading.orderbook.model.PriceLevel;
import com.activevector.trading.orderbook.model.PriceLevelFillStrategy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class OrderBook {
    private final String symbol;
    private final OrderStatusListener listener;
    private final PriceLevelFillStrategy priceLevelFillStrategy;

    private final NavigableMap<Long, PriceLevel> bid = new TreeMap<>(Comparator.<Long>naturalOrder().reversed());
    private final NavigableMap<Long, PriceLevel> ask = new TreeMap<>();

    public OrderBook(final String symbol, final OrderStatusListener listener) {
        this(symbol, listener, PriceLevelFillStrategy.FILL_IN_SEQ);
    }

    public OrderBook(final String symbol, final OrderStatusListener listener, final PriceLevelFillStrategy priceLevelFillStrategy) {
        this.symbol = requireNonNull(symbol);
        this.listener = listener;
        this.priceLevelFillStrategy = requireNonNull(priceLevelFillStrategy);
    }

    public final String getSymbol() {
        return symbol;
    }

    public Optional<Long> getBidPrice() {
        return getFirstKey(bid).filter(p -> !isMarketPriceIndicator(true, p));
    }

    public Optional<Long> getAskPrice() {
        return getFirstKey(ask).filter(p -> !isMarketPriceIndicator(false, p));
    }

    private static Optional<Long> getFirstKey(final NavigableMap<Long, PriceLevel> side) {
        return side.isEmpty() ? Optional.empty() : Optional.of(side.firstKey());
    }

    public boolean placeLimitOrder(
            final String orderId,
            final boolean buy,
            final int qty,
            final long price) {
        return placeOrder(orderId, buy, qty, price, true);
    }

    public boolean placeMarketOrder(final String orderId, final boolean buy, final int qty) {
        return placeOrder(orderId, buy, qty, getMarketPriceIndicator(buy), false);
    }

    private boolean placeOrder(
            final String orderId,
            final boolean buy,
            final int qty,
            final long price,
            final boolean allowFillWithMarketOrders) {
        final NavigableMap<Long, PriceLevel> matchingSide = (buy ? ask : bid);
        final NavigableMap<Long, PriceLevel> matchingLevels = new TreeMap<>(matchingSide.headMap(price, true));
        if (!allowFillWithMarketOrders) {
            for (final Iterator<Long> it = matchingLevels.keySet().iterator(); it.hasNext(); ) {
                final Long p = it.next();
                if (isMarketPriceIndicator(!buy, p)) {
                    it.remove();
                }
                else {
                    break;
                }
            }
        }
        if (matchingLevels.isEmpty()) {
            addOrder(orderId, buy, qty, price);
            return false;
        }

        int toBeFilled = qty;
        for (final Iterator<Map.Entry<Long, PriceLevel>> it = matchingLevels.entrySet().iterator(); toBeFilled > 0 && it.hasNext(); ) {
            final Map.Entry<Long, PriceLevel> entry = it.next();
            final Long fillPrice = entry.getKey();
            final PriceLevel priceLevel = entry.getValue();

            final int filled = priceLevel.provideFill(toBeFilled, fillPrice);
            toBeFilled -= filled;

            if (priceLevel.getTotalQty() == 0) {
                it.remove();
                matchingSide.remove(fillPrice);
            }

            if (listener != null) listener.orderFilled(orderId, fillPrice, filled, toBeFilled == 0);
        }

        if (toBeFilled > 0) {
            addOrder(orderId, buy, toBeFilled, price);
        }

        return true;
    }

    private void addOrder(
            final String orderId,
            final boolean buy,
            final int qty,
            final long price) {
        final NavigableMap<Long, PriceLevel> side = buy ? bid : ask;
        side.put(price, side.getOrDefault(price, new PriceLevel(listener, priceLevelFillStrategy)).add(orderId, qty));
    }

    public String renderAsString() {
        List<String[]> list = new ArrayList<>(Math.max(bid.size(), ask.size()));
        for (final Iterator<Map.Entry<Long, PriceLevel>> bidIt = bid.entrySet().iterator(), askIt = ask.entrySet().iterator();
             bidIt.hasNext() || askIt.hasNext(); ) {
            list.add(new String[] {getNextBookEntry(bidIt, true), getNextBookEntry(askIt, false)});
        }

        final int diffLen = 2;

        int sideLen = 0;
        for (final String[] arr : list) {
            sideLen = Math.max(sideLen, Math.max(arr[0].length(), arr[1].length()));
        }
        final int lineLen = diffLen + Math.max(sideLen, 3) * 2;

        final StringWriter sw = new StringWriter();
        try (final PrintWriter writer = new PrintWriter(sw)) {
            writer.printf("OrderBook for [%s]:%n", getSymbol());
            writer.println("-".repeat(lineLen));
            writer.print("BID");
            writer.print(" ".repeat(Math.max(sideLen - 3, 0) + diffLen));
            writer.print("ASK");
            writer.println(" ".repeat(Math.max(sideLen - 3, 0)));
            writer.println("-".repeat(lineLen));
            for (String[] arr : list) {
                writer.printf("%s%s%s%s%s%n",
                        arr[0], " ".repeat(Math.max(0, sideLen - arr[0].length())),
                        " ".repeat(diffLen),
                        arr[1], " ".repeat(Math.max(0, sideLen - arr[1].length())));
            }
            writer.println("-".repeat(lineLen));
        }
        return sw.toString();
    }

    private String getNextBookEntry(final Iterator<Map.Entry<Long, PriceLevel>> it, final boolean buy) {
        String str = "";
        if (it.hasNext()) {
            final Map.Entry<Long, PriceLevel> entry = it.next();

            final Long price = entry.getKey();
            final String strPrice = isMarketPriceIndicator(buy, price) ? "MP" : format("%.2f", price / 100D);

            str = format("%s,%s", strPrice, entry.getValue());
        }
        return str;
    }

    private long getMarketPriceIndicator(final boolean buy) {
        return buy ? Long.MAX_VALUE : 0L;
    }

    private boolean isMarketPriceIndicator(final boolean buy, final long price) {
        return price == getMarketPriceIndicator(buy);
    }
}
