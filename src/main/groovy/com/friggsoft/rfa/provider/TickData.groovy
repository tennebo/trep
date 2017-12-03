package com.friggsoft.rfa.provider

import com.friggsoft.rfa.simulator.TickSimulator
import com.friggsoft.rfa.simulator.TradedPrice
import com.reuters.rfa.common.Handle

/**
 * Tick-level data to be streamed.
 */
final class TickData {
    final TickSimulator tickSimulator

    boolean isPaused = false
    boolean attribInUpdates
    int priorityCount = 0
    int priorityClass = 0

    /**
     * The handle for the original request.
     */
    Handle handle

    TickData(TickSimulator tickSimulator) {
        this.tickSimulator = tickSimulator
    }

    @Override
    String toString() {
        String.format("TickData for %s", tickSimulator.ticker)
    }

    String getName() {
        return tickSimulator.ticker
    }

    TradedPrice current() {
        return tickSimulator.currentTick()
    }

    TradedPrice next() {
        return tickSimulator.nextTick()
    }

    /**
     * Return the # of ticks as a proxy for volume.
     */
    long getAcVol1() {
        return tickSimulator.count
    }
}
