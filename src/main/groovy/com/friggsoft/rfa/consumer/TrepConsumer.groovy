package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import com.friggsoft.rfa.config.Constants
import com.reuters.rfa.common.Context
import com.reuters.rfa.common.DeactivatedException
import com.reuters.rfa.common.DispatchQueueInGroupException
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.EventSource
import com.reuters.rfa.common.Handle
import com.reuters.rfa.config.ConfigDb
import com.reuters.rfa.config.ConfigProvider
import com.reuters.rfa.config.ConfigUtil
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.session.Session
import com.reuters.rfa.session.omm.OMMConsumer

/**
 * Market data consumer listening to streaming data from TREP via the RFA API.
 */
@Slf4j
final class TrepConsumer implements Closeable {

    /** Name of the service to request data from. */
    private final String serviceName

    /** TREP configuration database. */
    private final ConfigProvider configProvider

    /** The session serving this application. */
    private final Session session

    /** Event queue to use for receiving events from RFA to the application. */
    private final EventQueue eventQueue

    private final OMMConsumer ommConsumer
    private final OMMPool ommPool

    /** Manage this app's login to the TREP server. */
    private final LoginClient loginClient = new LoginClient()

    /** Handles item (RIC) requests and responses. */
    private ItemManager itemManager

    /** A handle returned by RFA. */
    private Handle loginHandle

    /**
     * Constructor that performs initialization.
     *
     * @param configDb database of configuration parameters
     */
    TrepConsumer(ConfigProvider configDb) {
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

        itemManager = new ItemManager(eventQueue, ommConsumer, ommPool)

        log.info("RFA version info: {}", Context.string())
    }

    @Override
    String toString() {
        return String.format("OMM %s Consumer", serviceName)
    }

    @Override
    void close() {
        log.info("Shutting down {}...", toString())

        eventQueue.deactivate()
        itemManager.close()
        if (loginHandle != null) {
            ommConsumer.unregisterClient(loginHandle)
        }
        eventQueue.destroy()
        ommConsumer.destroy()
        session.release()
        ommPool.destroy()

        Context.uninitialize()
        log.info("Shutdown complete")
    }

    /** Send a login request and wait for the response. */
    boolean login(long timeout, TimeUnit unit) {
        loginHandle = loginClient.sendLoginRequestAsync(configProvider, eventQueue, ommConsumer, ommPool)
        return loginClient.awaitLogin(timeout, unit)
    }

    void subscribe(String[] rics) {
        if (!loginClient.isLoggedIn()) {
            throw new RuntimeException("Login failed")
        }
        itemManager.sendRequest(loginHandle, serviceName, rics)
    }

    static Session acquireSession(ConfigProvider configDb) {
        String fullyQualifiedSessionName = configDb.variable("", "session")
        ConfigUtil.useDeprecatedRequestMsgs((ConfigDb)configDb, fullyQualifiedSessionName, false)
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
        final int waitMilliSeconds = 10

        for (; ;) {
            try {
                eventQueue.dispatch(waitMilliSeconds)
            }
            catch (DeactivatedException ignore) {
                // Normal shutdown
                log.info("Event queue deactivated")
                break
            }
            catch (DispatchQueueInGroupException ex) {
                // I have no idea what this is
                log.error("Event queue deactivated", ex)
                break
            }
            catch (RuntimeException ex) {
                // Some problem in our event handler
                log.error("Error while processing event: {}", ex.message, ex)
                break
            }
        }
    }

    void run(String[] rics) {
        // We have to start the dispatcher in order to log in;
        // run it on the default ForkJoinPool
        CompletableFuture<Void> dispatcherFuture = CompletableFuture.runAsync({ dispatchEvents() })

        boolean isLoggedIn = login(5, TimeUnit.SECONDS)
        if (isLoggedIn) {
            subscribe(rics)
            // Just sit here and wait for the dispatcher to exit
            dispatcherFuture.join()
        } else {
            log.error("Failed to log in: {}", loginClient.statusMessage)
            dispatcherFuture.cancel(true)
        }
    }
}
