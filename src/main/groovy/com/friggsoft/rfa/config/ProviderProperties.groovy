package com.friggsoft.rfa.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Connection properties for the provider (producer) app.
 */
@Component
@Profile(ProviderProperties.PROFILE_NAME)
@ConfigurationProperties(prefix = "trep", ignoreUnknownFields = false)
class ProviderProperties {

    static final String PROFILE_NAME = "provider"

    /** Name of the provider service. */
    String serviceName

    /** Namespace to put sessions and connection settings under. */
    String namespace

    /** Session to place connections under . */
    String sessionName

    /** Parent name to group connection parameters under. */
    String connectionName

    /** Connection parameter: Type of connection, e.g. RSSL_PROV. */
    String connectionType

    /** Connection parameter: Port number to listen to. */
    int portNumber
}
