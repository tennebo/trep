package com.friggsoft.rfa.config

import groovy.util.logging.Slf4j

import java.time.temporal.ChronoField
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import org.influxdb.InfluxDB
import org.influxdb.InfluxDBException
import org.influxdb.dto.Point
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import com.friggsoft.rfa.consumer.Quote

/**
 * Configure a market data sink (consumer).
 */
@Slf4j
@Configuration
class SinkConfig {

    /** Name of the measurement in the InfluxDB; this is akin to an SQL table. */
    final static String measurement = "quote"

    // Field names
    final static String price = "traded_price"

    // Tag names
    final static String ticker = "ticker"

    /**
     * A sink that will write to InfluxDB (a real-time DB).
     *
     * @param influxDB a connection to an InfluxDB instance
     * @param properties Spring Boot application properties
     * @return the sink (consumer) wired up to an InfluxDB instance
     */
    @Bean
    @Profile("influx")
    Consumer<Quote> influxSink(InfluxDB influxDB, InfluxDbProperties properties) {
        try {
            log.info("InfluxDB: {}", influxDB.ping().toString())
            log.info("Connecting to database '{}'", properties.database)
            influxDB.createDatabase(properties.database)
        } catch (InfluxDBException ex) {
            log.error("InfluxDB is down: {}", ex.message)
        }
        def writeQuote = writePoint.curry(influxDB, properties.database)
        return writeQuote
    }

    def writePoint = {
        InfluxDB influxDB, String database, Quote quote ->
            // We want the time expressed as unix epoch time in milliseconds
            long seconds = quote.time.toEpochSecond()
            long millis = quote.time.getLong(ChronoField.MILLI_OF_SECOND)
            long epochMillis = 1000 * seconds + millis

            log.debug("Time {} is {} ms ({} s + {} ms)", quote.time, epochMillis, seconds, millis)
            log.info("Writing {}={} to {}::{}", quote.ticker, quote.value, database, measurement)

            Point point = Point.measurement(measurement)
                    .time(epochMillis, TimeUnit.MILLISECONDS)
                    .addField(price, quote.value)
                    .tag(ticker, quote.ticker)
                    .build()
            influxDB.write(point)
    }

    @Bean
    @Profile("!influx")
    Consumer<Quote> logSink() {
        return logQuote
    }

    def logQuote = { Quote quote ->
        log.info("{}: {}", quote.ticker, quote.value)
    }
}
