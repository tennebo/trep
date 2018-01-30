package com.friggsoft.rfa.provider

import groovy.util.logging.Slf4j

import com.friggsoft.rfa.simulator.SimulationSpec
import com.friggsoft.rfa.simulator.TickSimulator
import com.friggsoft.rfa.simulator.TradedPrice
import com.friggsoft.rfa.util.GenericOMMParser
import com.reuters.rfa.common.Client
import com.reuters.rfa.common.Event
import com.reuters.rfa.common.Handle
import com.reuters.rfa.common.Token
import com.reuters.rfa.omm.OMMAttribInfo
import com.reuters.rfa.omm.OMMData
import com.reuters.rfa.omm.OMMElementEntry
import com.reuters.rfa.omm.OMMElementList
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMPriority
import com.reuters.rfa.rdm.RDMMsgTypes
import com.reuters.rfa.session.TimerIntSpec
import com.reuters.rfa.session.omm.OMMInactiveClientSessionEvent
import com.reuters.rfa.session.omm.OMMItemCmd
import com.reuters.rfa.session.omm.OMMSolicitedItemEvent

/**
 * Process client requests.
 */
@Slf4j
final class ProviderClientSession implements Client, Closeable {
    private final TrepProvider provider
    private final OmmMessageEncoder messageEncoder

    /**
     * A timer to periodically generate timer events to handles updates.
     */
    private Handle timerHandle

    /**
     * Collection of items receiving updates; key is request token.
     */
    private final HashMap<Token, TickData> itemRequestMap = new HashMap<>()

    /**
     * Map of RIC to request token.
     */
    private final HashMap<String, Token> itemRequestTokens = new HashMap<>()

    ProviderClientSession(TrepProvider providerApp) {
        this.provider = providerApp
        this.messageEncoder = new OmmMessageEncoder(
                provider.serviceName, provider.rwfDictionary, provider.getOmmPool())
    }

    @Override
    void close() {
        unregisterTimer()
        provider.unregisterClient(this)
        itemRequestMap.clear()
        itemRequestTokens.clear()
        messageEncoder.close()
    }

    @Override
    void processEvent(Event event) {
        assert event != null, 'event cannot be null'

        switch (event.type) {
            case Event.TIMER_EVENT:
                sendUpdatesOnTimer()
                break
            case Event.OMM_INACTIVE_CLIENT_SESSION_PUB_EVENT:
                processInactiveClientSessionEvent((OMMInactiveClientSessionEvent) event)
                break
            case Event.OMM_SOLICITED_ITEM_EVENT:
                processOMMSolicitedItemEvent((OMMSolicitedItemEvent) event)
                break
            default:
                log.error("Unhandled event type: {}", event.type)
                break
        }
    }

    private void sendUpdatesOnTimer() {
        for (Token rq : itemRequestMap.keySet()) {
            TickData tickData = itemRequestMap.get(rq)
            if (tickData == null || tickData.isPaused) { // Don't send update for paused items
                continue
            }

            TradedPrice tradedPrice = tickData.next()

            // Set the request token associated with this item into the OMMItemCmd
            OMMItemCmd cmd = new OMMItemCmd()
            cmd.setToken(rq)

            // Encode the update response message and set it into the OMMItemCmd
            OMMMsg updateMsg = messageEncoder.encodeUpdateMsg(tickData, tradedPrice)
            cmd.setMsg(updateMsg)
            submitCommand(cmd, "update")
            log.info("Sent update for {}", tradedPrice.toString())
        }
    }

    /**
     * Session was disconnected or closed.
     */
    private void processInactiveClientSessionEvent(OMMInactiveClientSessionEvent event) {
        log.info("Received OMMInactiveClientSessionEvent with handle: {}", event.getHandle())
        log.info(
                "Client position: {}/{}/{}",
                event.getClientIPAddress(),
                event.getClientHostName(),
                event.getListenerName())

        close()
    }

    /**
     * This event contains a request from consumer for processing.
     * Each event is associated with a particular Thomson Reuters defined
     * or customer defined domain.
     */
    private void processOMMSolicitedItemEvent(OMMSolicitedItemEvent event) {
        OMMMsg msg = event.getMsg()

        switch (msg.getMsgModelType()) {
            case RDMMsgTypes.LOGIN:
                // Reuters defined domain message model - LOGIN
                processLoginRequest(event)
                break
            case RDMMsgTypes.DIRECTORY:
                // Reuters defined domain message model - DIRECTORY
                processDirectoryRequest(event)
                break
            case RDMMsgTypes.DICTIONARY:
                // Reuters defined domain message model - DICTIONARY
                processDictionaryRequest(event)
                break
            default:
                // All other Reuters defined domain message models and
                // customer domain message model are considered items
                processItemRequest(event)
                break
        }
    }

    private void processLoginRequest(OMMSolicitedItemEvent event) {
        OMMMsg msg = event.getMsg()

        switch (msg.getMsgType()) {
            case OMMMsg.MsgType.REQUEST:
                if (msg.isSet(OMMMsg.Indication.NONSTREAMING)) {
                    log.error("ERROR: Received unsupported NONSTREAMING request")
                    break
                }
                log.info("Login request received")

                OMMAttribInfo attribInfo = msg.getAttribInfo()
                if (attribInfo.has(OMMAttribInfo.HAS_NAME)) {
                    log.info("username: {}", attribInfo.getName())
                } else {
                    log.info("No username")
                }

                // Data in attribInfo of LOGIN domain has been defined to be ElementList
                // ElementList is iterable; each ElementEntry can be accessed through the iterator
                OMMElementList elementList = (OMMElementList) attribInfo.getAttrib()
                for (
                        @SuppressWarnings("unchecked") Iterator<OMMElementEntry> iter = elementList.iterator();
                        iter.hasNext();) {
                    OMMElementEntry element = (OMMElementEntry) iter.next()
                    OMMData data = element.getData()
                    log.info("{}: {}", element.getName(), data.toString())
                }

                OMMItemCmd cmd = new OMMItemCmd()
                cmd.setToken(event.getRequestToken())

                // Encode a login response message and set it into the OMMItemCmd
                OMMMsg loginRespMsg = messageEncoder.encodeLoginRespMsg(
                        elementList, msg.isSet(OMMMsg.Indication.REFRESH))
                cmd.setMsg(loginRespMsg)
                submitCommand(cmd, "login response")
                break
            case OMMMsg.MsgType.CLOSE_REQ:
                // Closing the Login stream should cause items associated with that login to be closed.
                log.info("Logout received")
                close()
                break
            default:
                log.error("Received unsupported message type: {}", msg.getMsgType())
                break
        }
    }

    private void processDirectoryRequest(OMMSolicitedItemEvent event) {
        OMMMsg msg = event.getMsg()
        switch (msg.getMsgType()) {
            case OMMMsg.MsgType.REQUEST:
                log.info("Directory request received")

                OMMItemCmd cmd = new OMMItemCmd()
                cmd.setToken(event.getRequestToken())

                // Encode directory response message and set it into the OMMItemCmd
                OMMMsg directoryRespMsg = messageEncoder.encodeDirectoryRespMsg(event)
                cmd.setMsg(directoryRespMsg)
                submitCommand(cmd, "directory response")
                break
            case OMMMsg.MsgType.CLOSE_REQ:
                log.info("Directory close request")
                break
            default:
                log.error("Received unsupported message type: {}", msg.getMsgType())
                break
        }
    }

    private void processDictionaryRequest(OMMSolicitedItemEvent event) {
        OMMMsg msg = event.getMsg()

        switch (msg.getMsgType()) {
            case OMMMsg.MsgType.REQUEST:
                log.info("Dictionary request received")

                OMMAttribInfo attribInfo = msg.getAttribInfo()
                String name = attribInfo.getName()
                log.info("Dictionary name: {}", name)

                OMMItemCmd cmd = new OMMItemCmd()
                cmd.setToken(event.getRequestToken())

                // Encode dictionary response message and set it into the OMMItemCmd
                OMMMsg dictionaryRespMsg
                if (name.equalsIgnoreCase("rwffld")) {
                    dictionaryRespMsg = messageEncoder.encodeFldDictionary(event)
                } else { // "rwfenum"
                    dictionaryRespMsg = messageEncoder.encodeEnumDictionary(event)
                }
                cmd.setMsg(dictionaryRespMsg)
                submitCommand(cmd, "dictionary response")
                break
            case OMMMsg.MsgType.CLOSE_REQ:
                log.info("Dictionary close request")
                break
            default:
                log.error("Received unsupported message type: {}", msg.getMsgType())
                break
        }
    }

    /**
     * Accept incoming item requests.
     *
     * If there is at least one streaming item request, keep an update timer alive.
     */
    private void processItemRequest(OMMSolicitedItemEvent event) {
        OMMMsg msg = event.getMsg()
        Token requestToken = event.getRequestToken()
        TickData tickData = itemRequestMap.get(requestToken)

        switch (msg.getMsgType()) {
            case OMMMsg.MsgType.REQUEST:
                if (tickData == null) {
                    String name = msg.getAttribInfo().getName()
                    def simSpec = new SimulationSpec(name)
                    def simulator = new TickSimulator(simSpec)
                    tickData = new TickData(simulator)

                    String serviceName = msg.getAttribInfo().getServiceName()
                    tickData.setHasAttribInUpdates(msg.isSet(OMMMsg.Indication.ATTRIB_INFO_IN_UPDATES))

                    if (msg.isSet(OMMMsg.Indication.NONSTREAMING)) {
                        // Non-streaming request, don't add it to the request table
                        log.info("Received item non-streaming request for {}:{}", serviceName, name)
                    } else {
                        log.info("Received item streaming request for {}:{}", serviceName, name)
                        itemRequestMap.put(requestToken, tickData)
                        tickData.setHandle(event.getHandle())
                        tickData.setPriorityCount(1)
                        tickData.setPriorityClass(1)

                        // If this is the first streaming request, activate update timer
                        if (itemRequestMap.size() == 1) {
                            registerTimer()
                        }
                    }
                    GenericOMMParser.parse(msg)
                } else {
                    // Re-issue request
                    log.info("Received item reissue for {}", tickData.getName())
                    GenericOMMParser.parse(msg)
                }
                if (msg.has(OMMMsg.HAS_PRIORITY)) {
                    OMMPriority priority = msg.getPriority()
                    tickData.setPriorityClass(priority.getPriorityClass())
                    tickData.setPriorityCount(priority.getCount())
                }
                sendRefreshMsg(event, tickData)
                break
            case OMMMsg.MsgType.CLOSE_REQ:
                String name = msg.getAttribInfo().getName()
                log.info("Item close request for {}", name)
                if (tickData != null) {
                    // Remove the reference to the Token associated for the item
                    itemRequestMap.remove(requestToken)
                    if (itemRequestMap.isEmpty()) {
                        // No more items, kill the timer
                        unregisterTimer()
                    }
                }
                break
            default:
                log.error("Received unsupported message type: {}", msg.getMsgType())
                break
        }
    }

    private void sendRefreshMsg(OMMSolicitedItemEvent event, TickData tickData) {
        // create a new OMMItemCmd
        OMMItemCmd cmd = new OMMItemCmd()

        // Set the request token associated with this item into the OMMItemCmd
        cmd.setToken(event.getRequestToken())

        // Encode the refresh response message and set it into the OMMItemCmd
        OMMMsg refreshMsg = messageEncoder.encodeRefreshMsg(event, tickData, tickData.current())
        cmd.setMsg(refreshMsg)

        submitCommand(cmd, "refresh")
    }

    /**
     * Submit an OMMItemCmd to RFA; log on error.
     */
    private void submitCommand(OMMItemCmd cmd, String what) {
        if (provider.ommProvider.submit(cmd,null) == 0) {
            log.error("Attempted to submit {} message with inactive handle", what)
        } else {
            log.debug("Sent {} message", what)
        }
    }

    private void registerTimer() {
        if (timerHandle == null) {
            int updateIntervalMs = 1 * 1000
            TimerIntSpec timer = new TimerIntSpec()
            timer.setDelay(updateIntervalMs)
            timer.setRepeating(true)
            timerHandle = provider.ommProvider.registerClient(provider.eventQueue, timer, this, null)
        }
    }

    private void unregisterTimer() {
        if (timerHandle != null) {
            provider.ommProvider.unregisterClient(timerHandle)
            timerHandle = null
        }
    }
}
