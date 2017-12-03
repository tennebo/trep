package com.friggsoft.rfa.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import com.reuters.rfa.config.ConfigDb

/**
 * Configure TREP providers and consumers.
 */
@Configuration
class TrepConfigurer {

    static String fullSessionName(String namespace, String session) {
        return String.format("%s::%s", namespace, session)
    }

    @Bean
    @Profile(ProviderProperties.PROFILE_NAME)
    ConfigDb providerConfigDb(ProviderProperties properties) {
        def configDb = new ConfigDb()

        configDb.addVariable(Constants.session, fullSessionName(properties.namespace, properties.sessionName))
        configDb.addVariable(Constants.serviceName, properties.serviceName)

        String connectionListKey = String.format(
                "%s.Sessions.%s.connectionList", properties.namespace, properties.sessionName)
        configDb.addVariable(connectionListKey, properties.connectionName)

        // Connection settings
        String connectionsKey = String.format("%s.Connections.%s", properties.namespace, properties.connectionName)
        configDb.addVariable(String.format("%s.connectionType", connectionsKey), properties.connectionType)
        configDb.addVariable(String.format("%s.portNumber", connectionsKey), properties.portNumber.toString())

        return configDb
    }

    @Bean
    @Profile(ConsumerProperties.PROFILE_NAME)
    ConfigDb consumerConfigDb(ConsumerProperties properties) {
        def configDb = new ConfigDb()

        configDb.addVariable(Constants.application, properties.application)
        configDb.addVariable(Constants.user, properties.user)
        configDb.addVariable(Constants.session, fullSessionName(properties.namespace, properties.sessionName))
        configDb.addVariable(Constants.serviceName, properties.serviceName)

        String connectionListKey = String.format(
                "%s.Sessions.%s.connectionList", properties.namespace, properties.sessionName)
        configDb.addVariable(connectionListKey, properties.connectionName)

        // Connection settings
        String connectionsKey = String.format("%s.Connections.%s", properties.namespace, properties.connectionName)
        configDb.addVariable(String.format("%s.connectionType", connectionsKey), properties.connectionType)
        configDb.addVariable(String.format("%s.portNumber", connectionsKey), properties.portNumber.toString())
        configDb.addVariable(String.format("%s.serverList", connectionsKey), properties.serverList)

        configDb.addVariable(Constants.position, properties.position)
        configDb.addVariable(Constants.MessageModelType, properties.mmt)

        // TODO: I have no idea what these are for
        configDb.addVariable("sendView", "true")

        // 1: list of Integer fields for field list view;
        // 2: list of field names for element list view
        configDb.addVariable("viewType", "2")

        configDb.addVariable("sendReissue", "true")
        configDb.addVariable("reissueInterval", "15") // Interval between Reissues (in seconds)
        configDb.addVariable("reissueWithPAR", "true") // Reissue with Pause and Resume, alternating each reissue
        configDb.addVariable("reissueWithPriority", "true") // Reissue with different Priority each reissue
        configDb.addVariable("initialRequestPaused", "false") // Initial batch request has PAUSE_REQ indication?

        return configDb
    }
}
