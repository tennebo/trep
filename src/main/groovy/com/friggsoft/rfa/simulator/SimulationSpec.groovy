package com.friggsoft.rfa.simulator

/**
 * Holds the seed data for simulating an equity traded price.
 */
final class SimulationSpec {

    /** Default value for max # of milliseconds per tick. */
    static final int TICK_INTERVAL = 1000

    /** Default to 40% vol. */
    static final double SIGMA_ANNUAL = 0.40

    /** The equity ticker being simulated. */
    final String ticker

    /** Starting spot price. */
    final double initialSpot

    /** Annualized lognormal volatility */
    final double sigmaAnnual

    /** Max # of milliseconds between ticks for this equity. */
    final int nextTickMaxMillis

    SimulationSpec(String ticker) {
        this.ticker = ticker
        this.initialSpot = 100
        this.sigmaAnnual = SIGMA_ANNUAL
        this.nextTickMaxMillis = TICK_INTERVAL
    }

    SimulationSpec(
            String ticker,
            double initialSpot, double sigmaAnnual, int nextTickMaxMillis) {

        this.ticker = ticker
        this.initialSpot = initialSpot
        this.sigmaAnnual = sigmaAnnual
        this.nextTickMaxMillis = nextTickMaxMillis
    }
}
