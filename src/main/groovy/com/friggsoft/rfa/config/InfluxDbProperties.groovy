package com.friggsoft.rfa.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Connection properties for InfluxDB.
 */
@Component
@Profile("influx")
@ConfigurationProperties(prefix = "influx", ignoreUnknownFields = true)
class InfluxDbProperties {

    /** URL of the InfluxDB instance. */
    String url

    /** Login user. */
    String user

    /** Login password. */
    String password

    /** Database to connect to. */
    String database

    /** The retention policy used for writing points. */
    String retentionPolicy = "autogen"

    boolean enableBatch = true

    /** The max number of write actions to collect in a batch. */
    int batchSize = 1000

    /** The time to wait [ms] at most before flushing each batch. */
    int batchDurationMs = 100
}
