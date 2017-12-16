package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import com.friggsoft.rfa.util.GenericOMMParser
import com.reuters.rfa.common.Client
import com.reuters.rfa.common.Event
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.Handle
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.rdm.RDMInstrument
import com.reuters.rfa.rdm.RDMMsgTypes
import com.reuters.rfa.session.omm.OMMConsumer
import com.reuters.rfa.session.omm.OMMItemEvent
import com.reuters.rfa.session.omm.OMMItemIntSpec

/**
 * Handle item requests and responses between the consumer application and RFA.
 */
@Slf4j
final class ItemManager implements Client, Closeable {

    private final EventQueue eventQueue
    private final OMMConsumer ommConsumer
    private final OMMPool ommPool

    /** Handles returned by RFA on registering the items; used to identify the items. */
    private final ArrayList<Handle> itemHandles = new ArrayList<>()

    ItemManager(EventQueue eventQueue, OMMConsumer ommConsumer, OMMPool ommPool) {
        this.eventQueue = eventQueue
        this.ommConsumer = ommConsumer
        this.ommPool = ommPool
    }

    @Override
    void close() {
        for (Handle itemHandle : itemHandles) {
            ommConsumer.unregisterClient(itemHandle)
        }
        itemHandles.clear()
    }

    @Override
    void processEvent(Event event) {
        if (event.type == Event.COMPLETION_EVENT) {
            // Completion event indicates that the stream was closed by RFA
            log.info("Received COMPLETION_EVENT: {}", event.handle)

        } else if (event.type == Event.OMM_ITEM_EVENT) {
            log.info("Received OMM_ITEM_EVENT: {}", event.handle)
            OMMItemEvent ie = (OMMItemEvent) event
            OMMMsg respMsg = ie.msg
            log.info("Received event for {} with state {}", getRIC(respMsg), getStateText(respMsg))
            GenericOMMParser.parse(respMsg)

        } else {
            log.error("Received an unsupported Event type:: {}", event.type)
        }
    }

    /**
     * Extract the RIC from the given message.
     */
    private static String getRIC(OMMMsg msg) {
        return msg.has(OMMMsg.HAS_ATTRIB_INFO) ? msg.getAttribInfo().getName() : "<No RIC>"
    }

    /**
     * For diagnostics: Extract the state description from the given message.
     */
    private static String getStateText(OMMMsg msg) {
        return msg.has(OMMMsg.HAS_STATE)? msg.getState().getText() : "<stateless>"
    }

    /**
     * Create streaming request messages for items and register them with RFA.
     */
    void sendRequest(Handle loginHandle, String serviceName, String[] itemNames) {
        log.info("Sending item requests")

        short msgModelType = RDMMsgTypes.MARKET_PRICE
        int indicationFlags =
                OMMMsg.Indication.REFRESH |
                OMMMsg.Indication.ATTRIB_INFO_IN_UPDATES

        OMMItemIntSpec ommItemIntSpec = new OMMItemIntSpec()

        // Prepare item request message
        OMMMsg ommMsg = ommPool.acquireMsg()
        try {
            ommMsg.setMsgType(OMMMsg.MsgType.REQUEST)
            ommMsg.setMsgModelType(msgModelType)
            ommMsg.setIndicationFlags(indicationFlags)
            ommMsg.setPriority((byte) 1, 1)

            // Set OMMMsg with negotiated version info from login handle
            ommMsg.setAssociatedMetaInfo(loginHandle)

            // Register for each item
            for (int i = 0; i < itemNames.length; i++) {
                String itemName = itemNames[i]
                log.info("Subscribing to " + itemName)

                ommMsg.setAttribInfo(serviceName, itemName, RDMInstrument.NameType.RIC)

                // Set the message into interest spec
                ommItemIntSpec.setMsg(ommMsg)
                Handle itemHandle = ommConsumer.registerClient(eventQueue, ommItemIntSpec, this, null)
                itemHandles.add(itemHandle)
            }
        } finally {
            ommPool.releaseMsg(ommMsg)
        }
    }
}
