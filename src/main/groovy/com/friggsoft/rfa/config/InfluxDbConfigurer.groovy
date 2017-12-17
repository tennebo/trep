package com.friggsoft.rfa.config

import java.util.concurrent.TimeUnit

import org.influxdb.InfluxDB
import org.influxdb.impl.InfluxDBImpl
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import okhttp3.OkHttpClient

/**
 * Configure InfluxDB for writing.
 */
@Profile("influx")
@Configuration
class InfluxDbConfigurer {

    private final InfluxDbProperties properties
    private final OkHttpClient.Builder builder

    InfluxDbConfigurer(
            InfluxDbProperties properties,
            ObjectProvider<OkHttpClient.Builder> builder) {
        this.properties = properties
        this.builder = builder.getIfAvailable(OkHttpClient.Builder.metaClass.&invokeConstructor)
    }

    @Bean
    InfluxDB influxDB() {
        InfluxDBImpl influxDB = new InfluxDBImpl(
                properties.url,
                properties.user,
                properties.password,
                builder,
                properties.database,
                properties.retentionPolicy,
                InfluxDB.ConsistencyLevel.ONE)

        // Batching: Flush every X Points, at least every Y ms
        if (properties.enableBatch) {
            influxDB.enableBatch(properties.batchSize, properties.batchDurationMs, TimeUnit.MILLISECONDS)
        }
        return influxDB
    }
}
