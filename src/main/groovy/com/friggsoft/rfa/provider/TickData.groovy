package com.friggsoft.rfa.provider

import com.reuters.rfa.common.Handle

/**
 * Tick-level data to be streamed.
 */
final class TickData {
    private static final double bidAskSpread = 0.4

    /**
     * RIC.
     */
    String name

    double tradePrice1
    double bid
    double ask
    long acVol1

    boolean isPaused = false
    boolean attribInUpdates = false
    int priorityCount = 0
    int priorityClass = 0

    /**
     * The handle for the original request.
     */
    Handle handle

    TickData() {
        reset()
    }

    String toString() {
        String.format("%-6s: %5.2f  Bid %5.2f  Ask %5.2f  Volume %4d", name, tradePrice1, bid, ask, acVol1)
    }

    void reset() {
        tradePrice1 = 10.0
        bid = tradePrice1 - bidAskSpread / 2
        ask = bid + bidAskSpread
    }

    void increment() {
        acVol1 += 50
        tradePrice1 += 0.01
        bid += 0.01
        ask += 0.01

        if (tradePrice1 >= 100) {
            reset()
        }
    }
}
