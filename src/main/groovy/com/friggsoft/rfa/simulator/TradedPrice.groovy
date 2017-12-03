package com.friggsoft.rfa.simulator

import java.time.LocalDateTime

/**
 * Represent a single equity tick.
 */
final class TradedPrice {
    private static final double bidAskSpread = 0.4

    /** The RIC. */
    final String ticker

    /** Time of the price. */
    final LocalDateTime timestamp

    /** The traded price. */
    final double price

    /** The bid quote. */
    final double bid

    /** The ask quote. */
    final double ask

    TradedPrice(String ticker, double price) {
        this.ticker = ticker
        this.price = price
        this.timestamp = LocalDateTime.now()

        this.bid = price - bidAskSpread / 2
        this.ask = bid + bidAskSpread
    }

    @Override
    String toString() {
        return String.format("%-6s: %6.2f  Bid %6.2f  Ask %6.2f  @ %s", ticker, price, bid, ask, timestamp)
    }
}
