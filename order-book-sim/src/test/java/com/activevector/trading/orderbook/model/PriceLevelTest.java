package com.activevector.trading.orderbook.model;

import com.activevector.trading.orderbook.OrderFillEvent;
import com.activevector.trading.orderbook.testutils.OrderStatusAccumulator;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.activevector.trading.orderbook.model.PriceLevelFillStrategy.*;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PriceLevelTest {
    private static final Long PRICE = 500L;

    @Rule
    public final OrderStatusAccumulator accumulator = new OrderStatusAccumulator();

    @Test
    public void providesInSeqFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize() {
        List.of(Boolean.TRUE, Boolean.FALSE).forEach(fillSizeMoreThanQty -> {
            accumulator.reset();
            verifyFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize(new PriceLevel(accumulator, FILL_IN_SEQ), fillSizeMoreThanQty);

            assertThat(accumulator.getCancellations(), is(emptyList()));

            final List<OrderFillEvent> fills = accumulator.getFills();
            assertThat(fills, is(Arrays.asList(
                    new OrderFillEvent("A", PRICE, 10, true),
                    new OrderFillEvent("B", PRICE, 40, true),
                    new OrderFillEvent("C", PRICE, 20, true),
                    new OrderFillEvent("D", PRICE, 30, true))));
        });
    }

    @Test
    public void providesLowestQtyFirstFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize() {
        List.of(Boolean.TRUE, Boolean.FALSE).forEach(fillSizeMoreThanQty -> {
            accumulator.reset();
            verifyFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize(new PriceLevel(accumulator, LOWEST_QTY_FIRST), fillSizeMoreThanQty);

            assertThat(accumulator.getCancellations(), is(emptyList()));

            final List<OrderFillEvent> fills = accumulator.getFills();
            assertThat(fills, is(Arrays.asList(
                    new OrderFillEvent("A", PRICE, 10, true),
                    new OrderFillEvent("C", PRICE, 20, true),
                    new OrderFillEvent("D", PRICE, 30, true),
                    new OrderFillEvent("B", PRICE, 40, true))));
        });
    }

    @Test
    public void providesHighestQtyFirstFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize() {
        List.of(Boolean.TRUE, Boolean.FALSE).forEach(fillSizeMoreThanQty -> {
            accumulator.reset();
            verifyFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize(new PriceLevel(accumulator, HIGHEST_QTY_FIRST), fillSizeMoreThanQty);

            assertThat(accumulator.getCancellations(), is(emptyList()));

            final List<OrderFillEvent> fills = accumulator.getFills();
            assertThat(fills, is(Arrays.asList(
                    new OrderFillEvent("B", PRICE, 40, true),
                    new OrderFillEvent("D", PRICE, 30, true),
                    new OrderFillEvent("C", PRICE, 20, true),
                    new OrderFillEvent("A", PRICE, 10, true))));
        });
    }

    private void verifyFillWhenPriceLevelQtyIsLessThanOrEqualToFillSize(final PriceLevel priceLevel, final boolean fillSizeMoreThanQty) {
        priceLevel.add("A", 10);
        priceLevel.add("B", 40);
        priceLevel.add("C", 20);
        priceLevel.add("D", 30);

        assertThat(priceLevel.getOrderCount(), is(4));
        assertThat(priceLevel.getTotalQty(), is(100));

        final int fill = priceLevel.provideFill(fillSizeMoreThanQty ? 150 : 100, PRICE);
        assertThat(fill, is(100));

        assertThat(priceLevel.getOrderCount(), is(0));
        assertThat(priceLevel.getTotalQty(), is(0));
    }

    @Test
    public void providesInSeqFillWhenPriceLevelQtyIsMoreThanFillSize() {
        verifyFillWhenPriceLevelQtyIsMoreThanFillSize(new PriceLevel(accumulator, FILL_IN_SEQ), 1);

        assertThat(accumulator.getCancellations(), is(emptyList()));

        final List<OrderFillEvent> fills = accumulator.getFills();
        assertThat(fills, is(Arrays.asList(
                new OrderFillEvent("A", PRICE, 10, true),
                new OrderFillEvent("B", PRICE, 40, true),
                new OrderFillEvent("C", PRICE, 20, true))));
    }

    @Test
    public void providesLowestQtyFirstFillWhenPriceLevelQtyIsMoreThanFillSize() {
        verifyFillWhenPriceLevelQtyIsMoreThanFillSize(new PriceLevel(accumulator, LOWEST_QTY_FIRST), 1);

        assertThat(accumulator.getCancellations(), is(emptyList()));

        final List<OrderFillEvent> fills = accumulator.getFills();
        assertThat(fills, is(Arrays.asList(
                new OrderFillEvent("A", PRICE, 10, true),
                new OrderFillEvent("C", PRICE, 20, true),
                new OrderFillEvent("D", PRICE, 30, true),
                new OrderFillEvent("B", PRICE, 10, false))));
    }

    @Test
    public void providesHighestQtyFirstFillWhenPriceLevelQtyIsMoreThanFillSize() {
        verifyFillWhenPriceLevelQtyIsMoreThanFillSize(new PriceLevel(accumulator, HIGHEST_QTY_FIRST), 2);

        assertThat(accumulator.getCancellations(), is(emptyList()));

        final List<OrderFillEvent> fills = accumulator.getFills();
        assertThat(fills, is(Arrays.asList(
                new OrderFillEvent("B", PRICE, 40, true),
                new OrderFillEvent("D", PRICE, 30, true))));
    }

    private void verifyFillWhenPriceLevelQtyIsMoreThanFillSize(final PriceLevel priceLevel, final int expectedRemainingOrderCount) {
        priceLevel.add("A", 10);
        priceLevel.add("B", 40);
        priceLevel.add("C", 20);
        priceLevel.add("D", 30);

        assertThat(priceLevel.getOrderCount(), is(4));
        assertThat(priceLevel.getTotalQty(), is(100));

        final int fill = priceLevel.provideFill(70, PRICE);
        assertThat(fill, is(70));

        assertThat(priceLevel.getOrderCount(), is(expectedRemainingOrderCount));
        assertThat(priceLevel.getTotalQty(), is(30));
    }
}
