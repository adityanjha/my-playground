package com.activevector.trading.orderbook;

import com.activevector.trading.orderbook.testutils.OrderStatusAccumulator;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderBookTest {
    private static final String SYMBOL = "ABC";

    private static final Random R = new Random();
    private static final AtomicInteger ID = new AtomicInteger(1);

    private static String nextOrderId() {
        return "Order" + ID.getAndIncrement();
    }

    @Rule
    public final OrderStatusAccumulator accumulator = new OrderStatusAccumulator();

    @Test
    public void test() {
        OrderBook book = new OrderBook(SYMBOL, accumulator);

        for (int i = 0, count = 10 + R.nextInt(11); i < count; i++) {
            final boolean buy = R.nextBoolean();
            final int qty = 10 * (1 + R.nextInt(9));
            final long price = 10000L + (buy ? -1 : 1) * (1 + R.nextInt(10));
            book.placeLimitOrder(nextOrderId(), buy, qty, price);
        }
        System.out.println(book.renderAsString());

        placeOrderAndShowMatchingPriceLevels(book, true, false);
        placeOrderAndShowMatchingPriceLevels(book, false, false);

        placeOrderAndShowMatchingPriceLevels(book, true, true);
        placeOrderAndShowMatchingPriceLevels(book, false, true);
    }

    private void placeOrderAndShowMatchingPriceLevels(
            final OrderBook book,
            final boolean buy,
            final boolean marketOrder) {
        System.out.println();
        accumulator.reset();

        final String orderId = nextOrderId();
        final int qty = 10 * (5 + R.nextInt(9));

        final boolean orderMatched;
        if (marketOrder) {
            System.out.printf("Placing market-order: %s,%s,MP,%d%n", orderId, buy ? "BID" : "ASK", qty);
            orderMatched = book.placeMarketOrder(orderId, buy, qty);
        }
        else {
            final Long bestMatchingPrice = (buy ? book.getAskPrice() : book.getBidPrice()).orElse(10000L);
            final long price = bestMatchingPrice + (buy ? 1 : -1) * R.nextInt(10);
            System.out.printf("Placing order: %s,%s,%.2f,%d%n", orderId, buy ? "BID" : "ASK", price / 100D, qty);
            orderMatched = book.placeLimitOrder(orderId, buy, qty, price);
        }

        System.out.println("Order matched: " + orderMatched);
        System.out.println("Fills notified: ");
        accumulator.getFills().stream().map(OrderFillEvent::toString).forEach(s -> System.out.println("  " + s));

        System.out.println(book.renderAsString());
    }
}
