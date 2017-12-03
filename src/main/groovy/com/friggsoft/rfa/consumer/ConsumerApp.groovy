package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import com.friggsoft.rfa.config.Constants
import com.reuters.rfa.common.Context
import com.reuters.rfa.common.DispatchException
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.EventSource
import com.reuters.rfa.common.Handle
import com.reuters.rfa.config.ConfigDb
import com.reuters.rfa.config.ConfigProvider
import com.reuters.rfa.config.ConfigUtil
import com.reuters.rfa.omm.OMMEncoder
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.session.Session
import com.reuters.rfa.session.omm.OMMConsumer

/**
 * Market data consumer listening to streaming data from TREP via the RFA API.
 */
@Slf4j
final class ConsumerApp implements Closeable {

    /** Event queue to use for receiving events from RFA to the application. */
    final EventQueue eventQueue

    /** Name of the service to request data from. */
    final String serviceName

    /** TREP configuration database. */
    private final ConfigProvider configProvider

    final OMMConsumer ommConsumer
    final OMMPool ommPool
    final OMMEncoder encoder

    /** The session serving this application. */
    private final Session session

    private final LoginClient loginClient = new LoginClient()
    private ItemManager itemManager

    /** A handle returned by RFA. */
    private Handle loginHandle

    /**
     * Constructor that performs initialization.
     *
     * @param configDb database of configuration parameters
     */
    ConsumerApp(ConfigDb configDb) {
        log.info("Initializing OMM Consumer...")

        configProvider = configDb
        serviceName = configDb.variable("", Constants.serviceName)
        String application = configDb.variable("", Constants.application)
        String username = configDb.variable("", Constants.user)
        log.info("ServiceName={}, application={}, user={}", serviceName, application, username)

        // Initialize context; provide an explicit ConfigProvider to avoid hitting the Windows Registry
        Context.initialize(configDb)

        // Create event queue
        eventQueue = EventQueue.create("OMMConsumer EventQueue")

        // Acquire session
        session = acquireSession(configDb)

        // Create an OMMConsumer event source
        ommConsumer = (OMMConsumer) session.createEventSource(EventSource.OMM_CONSUMER, "OMMConsumer Client", true)

        // Create an OMMPool
        ommPool = OMMPool.create()

        // Create an OMMEncoder
        encoder = ommPool.acquireEncoder()
    }

    @Override
    void close() {
        log.info("Shutting down OMM Consumer...")

        eventQueue.deactivate()
        if (itemManager != null) {
            itemManager.close()
        }
        if (loginHandle != null) {
            ommConsumer.unregisterClient(loginHandle)
        }
        eventQueue.destroy()
        ommConsumer.destroy()
        session.release()
        ommPool.releaseEncoder(encoder)
        ommPool.destroy()

        Context.uninitialize()
        log.info("Shutdown complete")
    }

    /** Send a login request. */
    boolean login(long timeout, TimeUnit unit) {
        loginHandle = loginClient.sendLoginRequestAsync(configProvider, eventQueue, ommConsumer, ommPool)
        return loginClient.awaitLogin(timeout, unit)
    }

    void subscribe() {
        itemManager = new ItemManager(this)
        itemManager.sendRequest(loginHandle, serviceName)
    }

    static Session acquireSession(ConfigDb configDb) {
        String fullyQualifiedSessionName = configDb.variable("", "session")
        ConfigUtil.useDeprecatedRequestMsgs(configDb, fullyQualifiedSessionName, false)
        Session session = Session.acquire(fullyQualifiedSessionName)
        if (session == null) {
            String msg = String.format("Failed acquire session %s", fullyQualifiedSessionName)
            throw new RuntimeException(msg)
        }
        return session
    }

    /**
     * Dispatch events. If no events, then wait N milliseconds before trying again.
     */
    def dispatchEvents = { ->
        final int waitMilliSeconds = 100

        for (; ;) {
            try {
                eventQueue.dispatch(waitMilliSeconds)
            }
            catch (DispatchException ex) {
                log.warn("Event queue deactivated", ex)
                break
            }
        }
    }

    void run() {
        // We have to start the dispatcher in order to log in;
        // run it on the default ForkJoinPool
        CompletableFuture<Void> dispatcherFuture = CompletableFuture.runAsync({ dispatchEvents() })

        boolean isLoggedIn = login(5, TimeUnit.SECONDS)
        if (isLoggedIn) {
            subscribe()
            // Just sit here and wait for the dispatcher to exit
            dispatcherFuture.join()
        } else {
            log.error("Failed to log in")
            dispatcherFuture.cancel(true)
        }
    }
}