package com.friggsoft.rfa.consumer

import com.reuters.rfa.common.Context
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.EventSource
import com.reuters.rfa.config.ConfigProvider
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.session.Session
import com.reuters.rfa.session.omm.OMMConsumer

/**
 * A class to hold all objects needed to connect to TREP.
 */
final class TrepConnector implements AutoCloseable {

    /** TREP configuration database. */
    final ConfigProvider configProvider

    /** Event queue to use for receiving events from RFA to the application. */
    final EventQueue eventQueue

    /** The session serving this application. */
    final Session session

    /** An OMMConsumer event source.  */
    final OMMConsumer ommConsumer

    final OMMPool ommPool

    /**
     * Create a new connector using the given configuration provider.
     *
     * @param configProvider TREP configuration database
     */
    TrepConnector(ConfigProvider configProvider) {
        Context.initialize(configProvider)

        this.configProvider = configProvider

        ommPool = OMMPool.create()
        eventQueue = EventQueue.create("Consumer EventQueue")
        session = acquireSession()
        ommConsumer = (OMMConsumer) session.createEventSource(
                EventSource.OMM_CONSUMER, "Consumer Client", true)
    }

    @Override
    void close() {
        eventQueue.deactivate()
        eventQueue.destroy()
        ommConsumer.destroy()
        session.release()
        ommPool.destroy()

        Context.uninitialize()
    }

    private Session acquireSession() {
        return ConsumerApp.acquireSession(configProvider)
    }
}
