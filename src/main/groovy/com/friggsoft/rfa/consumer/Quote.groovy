package com.friggsoft.rfa.consumer

import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Holds one market price quote.
 */
final class Quote {

    /** Reuters RIC. */
    final String ticker

    /** The last traded price. */
    final double value

    /** The timestamp of this quote. */
    final OffsetDateTime time

    Quote(String ticker, double value) {
        this(ticker, value, OffsetDateTime.now(ZoneOffset.UTC))
    }

    Quote(String ticker, double value, OffsetDateTime time) {
        this.ticker = ticker
        this.value = value
        this.time = time
    }
}
