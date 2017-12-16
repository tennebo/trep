package com.friggsoft.rfa.consumer

import com.friggsoft.rfa.config.Constants
import com.reuters.rfa.config.ConfigDb
import com.reuters.rfa.config.ConfigProvider

/**
 * Create a {@link ConfigProvider} for unit testing.
 */
final class ConfigProviderFactory {

    private static final String namespace = "namespace"
    private static final String sessionName = "session"
    private static final String connectionName = "test-connection"

    static ConfigProvider newConfigProvider() {
        ConfigDb configDb = new ConfigDb()

        configDb.addVariable(Constants.application, "616")
        configDb.addVariable(Constants.user, "eve")
        configDb.addVariable(Constants.position, "1.1.1.1/net")
        configDb.addVariable(Constants.session, namespace + "::" + sessionName)
        configDb.addVariable(Constants.serviceName, "TEST")

        String connectionListKey = String.format(
                "%s.Sessions.%s.connectionList", namespace, sessionName)
        configDb.addVariable(connectionListKey, connectionName)

        // Connection settings
        String connectionsKey = String.format("%s.Connections.%s", namespace, connectionName)
        configDb.addVariable(String.format("%s.connectionType", connectionsKey), "RSSL")
        configDb.addVariable(String.format("%s.portNumber", connectionsKey), "" + "6666")
        configDb.addVariable(String.format("%s.serverList", connectionsKey), "localhost")

        // What type of data are we requesting?
        configDb.addVariable(Constants.MessageModelType, "MARKET_PRICE")

        // Disable logging
        configDb.addVariable(Constants.logFileName, "none")

        return configDb
    }
}
