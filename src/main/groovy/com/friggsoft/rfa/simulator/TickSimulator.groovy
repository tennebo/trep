package com.friggsoft.rfa.simulator

/**
 * Simulate tick-by-tick evolution of an equity.
 */
final class TickSimulator {

    /** The equity ticker being simulated. */
    final String ticker

    /** Convert annual vol to vol per second. */
    private final double sigmaPerSecond

    /** Used for the equity process. */
    private final Random random = new Random()

    int count
    private double currentSpot

    TickSimulator(SimulationSpec simSpec) {
        this.ticker = simSpec.ticker
        this.currentSpot = simSpec.initialSpot

        // Convert annual vol to vol per second
        this.sigmaPerSecond = simSpec.sigmaAnnual / Math.sqrt(252 * 8 * 3600)
    }

    /**
     * Create the next quote.
     * This function has side-effects.
     *
     * @return the next simulated quote
     */
    TradedPrice nextTick() {
        double delta = random.nextGaussian() * sigmaPerSecond * currentSpot
        currentSpot += delta
        count++

        // Round to two decimals
        double roundedSpot = Math.round(currentSpot * 100) / 100d
        return new TradedPrice(ticker, roundedSpot)
    }
}
