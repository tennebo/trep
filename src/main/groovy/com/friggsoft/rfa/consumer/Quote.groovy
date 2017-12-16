package com.friggsoft.rfa.consumer

/**
 * Holds one market price.
 */
final class Quote {

    Quote(String ticker, double value) {
        this.ticker = ticker
        this.value = value
    }

    /** Reuters RIC. */
    final String ticker

    /** The last traded price. */
    final double value
}
