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
    ConfigDb providerConfigDb(ProviderProperties props) {
        def configDb = new ConfigDb()

        configDb.addVariable(Constants.session, fullSessionName(props.namespace, props.sessionName))
        configDb.addVariable(Constants.serviceName, props.serviceName)

        String connectionListKey = String.format(
                "%s.Sessions.%s.connectionList", props.namespace, props.sessionName)
        configDb.addVariable(connectionListKey, props.connectionName)

        // Connection settings
        String connectionsKey = String.format("%s.Connections.%s", props.namespace, props.connectionName)
        configDb.addVariable(String.format("%s.connectionType", connectionsKey), props.connectionType)
        configDb.addVariable(String.format("%s.connectionTimeout", connectionsKey), props.connectionTimeout.toString())
        configDb.addVariable(String.format("%s.portNumber", connectionsKey), props.portNumber.toString())

        // Logging and tracing
        configDb.addVariable(Constants.logFileName, props.logfileName)
        configDb.addVariable(Constants.mountTrace, props.mountTrace.toString())

        return configDb
    }

    @Bean
    @Profile(ConsumerProperties.PROFILE_NAME)
    ConfigDb consumerConfigDb(ConsumerProperties props) {
        def configDb = new ConfigDb()

        configDb.addVariable(Constants.application, props.application)
        configDb.addVariable(Constants.user, props.user)
        configDb.addVariable(Constants.session, fullSessionName(props.namespace, props.sessionName))
        configDb.addVariable(Constants.serviceName, props.serviceName)

        String connectionListKey = String.format(
                "%s.Sessions.%s.connectionList", props.namespace, props.sessionName)
        configDb.addVariable(connectionListKey, props.connectionName)

        // Connection settings
        String connectionsKey = String.format("%s.Connections.%s", props.namespace, props.connectionName)
        configDb.addVariable(String.format("%s.connectionType", connectionsKey), props.connectionType)
        configDb.addVariable(String.format("%s.connectionTimeout", connectionsKey), props.connectionTimeout.toString())
        configDb.addVariable(String.format("%s.portNumber", connectionsKey), props.portNumber.toString())
        configDb.addVariable(String.format("%s.serverList", connectionsKey), props.serverList)

        configDb.addVariable(Constants.position, props.position)
        configDb.addVariable(Constants.MessageModelType, props.mmt)

        // Logging and tracing
        configDb.addVariable(Constants.logFileName, props.logfileName)
        configDb.addVariable(Constants.mountTrace, props.mountTrace.toString())

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
