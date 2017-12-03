package com.friggsoft.rfa.provider

import groovy.util.logging.Slf4j

import com.friggsoft.rfa.config.Constants
import com.reuters.rfa.common.Client
import com.reuters.rfa.common.Context
import com.reuters.rfa.common.DispatchException
import com.reuters.rfa.common.Event
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.EventSource
import com.reuters.rfa.common.Handle
import com.reuters.rfa.config.ConfigDb
import com.reuters.rfa.config.ConfigUtil
import com.reuters.rfa.dictionary.FieldDictionary
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.session.Session
import com.reuters.rfa.session.omm.OMMActiveClientSessionEvent
import com.reuters.rfa.session.omm.OMMClientSessionIntSpec
import com.reuters.rfa.session.omm.OMMCmdErrorEvent
import com.reuters.rfa.session.omm.OMMErrorIntSpec
import com.reuters.rfa.session.omm.OMMListenerEvent
import com.reuters.rfa.session.omm.OMMListenerIntSpec
import com.reuters.rfa.session.omm.OMMProvider

/**
 * An RFA OMM Provider accepting connections from OMM Consumers (RFA).
 *
 * It handles:
 * - login requests
 * - directory requests
 * - item requests
 *
 * After initialization, the app will periodically send events (if streaming was requested).
 */
@Slf4j
final class ProviderApp implements Client, Closeable {

    /** Name of the service that this provider application supports. */
    final String serviceName

    /** A collection to hold accepted client sessions. */
    final HashMap<ProviderClientSession, Handle> clientSessions = new HashMap<>()

    final FieldDictionary rwfDictionary = FieldDictionary.create()

    /**
     * Event queue to use for passing events from RFA to the application.
     */
    final EventQueue eventQueue

    /**
     * A memory pool managed by RFA to use for application created message objects.
     */
    OMMPool ommPool

    /** An event source representing this application in the session. */
    OMMProvider ommProvider

    /** The session serving this application. */
    private final Session session

    /**
     * A handle returned by RFA on registering the provider to the listener interest.
     */
    private Handle csListenerIntSpecHandle

    /**
     * A handle returned by RFA on registering the client to the error interest.
     */
    private Handle errIntSpecHandle

    /**
     * Constructor that performs initialization.
     *
     * @param configProvider database of configuration parameters
     */
    ProviderApp(ConfigDb configProvider) {
        log.info("Initializing OMM Provider...")

        serviceName = configProvider.variable("", Constants.serviceName)
        log.info("Service name is {}", serviceName)

        // Initialize context; provide an explicit ConfigProvider to avoid hitting the Windows Registry
        Context.initialize(configProvider)

        eventQueue = EventQueue.create("OMMProvider Server EventQueue")
        DictionaryReader.load(rwfDictionary)

        // Acquire a session
        String fullyQualifiedSessionName = configProvider.variable("", Constants.session)
        ConfigUtil.useDeprecatedRequestMsgs(configProvider, fullyQualifiedSessionName,false)
        session = Session.acquire(fullyQualifiedSessionName)
        if (session == null) {
            String msg = String.format("Failed to acquire session %s", fullyQualifiedSessionName)
            throw new RuntimeException(msg)
        }

        ommProvider = (OMMProvider) session.createEventSource(EventSource.OMM_PROVIDER, "OMMProvider Server")

        // Register for listener events. OMMListenerIntSpec is used to register interest for
        // any incoming sessions specified on the port provided by the configuration.
        OMMListenerIntSpec listenerIntSpec = new OMMListenerIntSpec()
        listenerIntSpec.setListenerName("")
        csListenerIntSpecHandle = ommProvider.registerClient(eventQueue, listenerIntSpec, this, null)

        // Register interest for any error events during the publishing cycle
        OMMErrorIntSpec errIntSpec = new OMMErrorIntSpec()
        errIntSpecHandle = ommProvider.registerClient(eventQueue, errIntSpec, this, null)

        // Create an OMMPool
        ommPool = OMMPool.create()

        log.info(Context.string())
        log.info("Initialization complete; awaiting incoming client sessions")
    }

    /**
     * Clean up all resources on application shutdown.
     *
     * This must mirror the calling sequence of the constructor.
     */
    @Override
    void close() {
        log.info("Shutting down OMM Provider...")

        eventQueue.deactivate()
        ommProvider.unregisterClient(errIntSpecHandle)
        ommProvider.unregisterClient(csListenerIntSpecHandle)
        ommProvider.destroy()
        eventQueue.destroy()
        session.release()
        ommPool.destroy()

        Context.uninitialize()
        log.info("Shutdown complete")
    }

    /**
     * Callback for asynchronous session events.
     */
    @Override
    void processEvent(Event event) {
        assert event != null, 'event cannot be null'

        switch (event.getType()) {
            case Event.OMM_ACTIVE_CLIENT_SESSION_PUB_EVENT:
                processActiveClientSessionEvent((OMMActiveClientSessionEvent) event)
                break
            case Event.OMM_LISTENER_EVENT:
                OMMListenerEvent listenerEvent = (OMMListenerEvent) event
                log.info("Received OMM LISTENER EVENT: " + listenerEvent.getListenerName())
                log.info(listenerEvent.getStatus().toString())
                break
            case Event.OMM_CMD_ERROR_EVENT:
                processOMMCmdErrorEvent((OMMCmdErrorEvent) event)
                break
            default:
                log.error("Unhandled event type: " + event.getType())
                break
        }
    }

    /**
     * Handle events of type OMM_CMD_ERROR_EVENT.
     *
     * In case of failure to publish the data to the network,
     * OMMCmdErrorEvent will be sent back to the provider application.
     */
    private static void processOMMCmdErrorEvent(OMMCmdErrorEvent errorEvent) {
        log.error(
                "Received OMMCmd Error Event for Cmd ID {}: {}",
                errorEvent.getCmdID(),
                errorEvent.getStatus().getStatusText())
    }

    /**
     * Handle events of type OMM_ACTIVE_CLIENT_SESSION_PUB_EVENT.
     */
    private void processActiveClientSessionEvent(OMMActiveClientSessionEvent event) {
        log.info("Received OMMActiveClientSessionEvent with handle: {}", event.getHandle())
        log.info(
                "Client position: {}/{}/{}",
                event.getClientIPAddress(),
                event.getClientHostName(),
                event.getListenerName())

        Handle csHandle = event.getClientSessionHandle()
        registerClient(csHandle)
    }

    /**
     * Create and register a client session.
     */
    private void registerClient(Handle handle) {
        OMMClientSessionIntSpec intSpec = new OMMClientSessionIntSpec()
        intSpec.setClientSessionHandle(handle)
        ProviderClientSession pcs = new ProviderClientSession(this)

        // Accept a session through registerClient; this returns a client session handle
        Handle clientSessionHandle = ommProvider.registerClient(eventQueue, intSpec, pcs, null)

        // Add it to the map
        clientSessions.put(pcs, clientSessionHandle)
    }

    /**
     * Unregister a client session.
     */
    void unregisterClient(ProviderClientSession pcs) {
        assert pcs != null, 'pcs cannot be null'

        if (clientSessions.containsKey(pcs)) {
            Handle clientSessionHandle = clientSessions.get(pcs)
            ommProvider.unregisterClient(clientSessionHandle)
            clientSessions.remove(pcs)
        } else {
            log.warn("Client {} was not registered", pcs)
        }
    }

    /**
     * Dispatch events from event queue. If no events, wait N milliseconds before trying again.
     */
    void dispatchEvents() {
        final int waitMilliSeconds = 100

        for (;;) {
            try {
                eventQueue.dispatch(waitMilliSeconds)
            }
            catch (DispatchException ex) {
                log.error("Event queue deactivated", ex)
                break
            }
        }
    }
}
