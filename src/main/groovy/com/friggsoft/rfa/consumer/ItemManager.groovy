package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import com.reuters.rfa.common.Client
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.Handle
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.rdm.RDMInstrument
import com.reuters.rfa.rdm.RDMMsgTypes
import com.reuters.rfa.session.omm.OMMConsumer
import com.reuters.rfa.session.omm.OMMItemIntSpec

/**
 * Handle item requests from the consumer application to RFA.
 */
@Slf4j
final class ItemManager implements Closeable {

    private final EventQueue eventQueue
    private final OMMConsumer ommConsumer
    private final OMMPool ommPool

    /** Handles returned by RFA on registering the items. */
    private final Map<String, Handle> itemHandles = new HashMap<>()

    ItemManager(EventQueue eventQueue, OMMConsumer ommConsumer, OMMPool ommPool) {
        this.eventQueue = eventQueue
        this.ommConsumer = ommConsumer
        this.ommPool = ommPool
    }

    @Override
    void close() {
        for (String item : itemHandles.keySet()) {
            ommConsumer.unregisterClient(itemHandles.get(item))
        }
        itemHandles.clear()
    }

    /**
     * Create streaming request messages for items and register them with RFA.
     */
    void sendSubscribeRequest(Handle loginHandle, String serviceName, String[] itemNames, Client client) {
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
                log.info("Subscribing to {}", itemName)

                ommMsg.setAttribInfo(serviceName, itemName, RDMInstrument.NameType.RIC)

                // Set the message into interest spec
                ommItemIntSpec.setMsg(ommMsg)
                Handle itemHandle = ommConsumer.registerClient(eventQueue, ommItemIntSpec, client, null)
                itemHandles.put(itemName, itemHandle)
            }
        } finally {
            ommPool.releaseMsg(ommMsg)
        }
    }
}
