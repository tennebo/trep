package com.friggsoft.rfa.config

import groovy.util.logging.Slf4j

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
            log.info("Writing {}={} to InfluxDB {}", quote.ticker, quote.value, database)
            Point point = Point.measurement(quote.ticker)
                    .addField("value", quote.value)
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
