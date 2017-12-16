package com.friggsoft.rfa.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Connection properties for the consumer app.
 */
@Component
@Profile(ConsumerProperties.PROFILE_NAME)
@ConfigurationProperties(prefix = "trep", ignoreUnknownFields = false)
class ConsumerProperties {

    static final String PROFILE_NAME = "consumer"

    /** Application name. No idea what this is for. */
    String application

    /** Username to log in to the service as. */
    String user

    /** Name of the service to connect to. */
    String serviceName

    /** Namespace to put sessions and connection settings under. */
    String namespace

    /** Session to place connections under . */
    String sessionName

    /** Parent name to group connection parameters under. */
    String connectionName

    /** Connection parameter: Type of connection, e.g. RSSL. */
    String connectionType

    /**
     * The time [milliseconds] to wait for a connection attempt to succeed.
     * After a connection has been established, this value is used to set a
     * timer to check for three consecutive missed pings.
     */
    int connectionTimeout

    /** Connection parameter: List of servers to connect to. */
    String serverList

    /** Connection parameter: Port number to connect to. */
    int portNumber

    /** For DACS. */
    String position

    /** Message Model Type. */
    String mmt

    /** Logfile name, see {@link Constants#logFileName}. */
    String logfileName

    /** Enable tracing of the connection process? */
    boolean mountTrace
}
