package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import java.util.function.Consumer

import com.reuters.rfa.common.Client
import com.reuters.rfa.common.Event
import com.reuters.rfa.omm.OMMData
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMTypes
import com.reuters.rfa.session.omm.OMMItemEvent

/**
 * Process incoming market data update events.
 */
@Slf4j
final class StreamingClient implements Client {
    private final OmmPayloadParser ommPayloadParser = new OmmPayloadParser()
    private final Consumer<Quote> sink

    StreamingClient(Consumer<Quote> sink) {
        this.sink = sink
        assert sink != null, "sink cannot be null"
    }

    /**
     * For diagnostics: Extract the state description from the given message.
     */
    private static String getStateText(OMMMsg msg) {
        return msg.has(OMMMsg.HAS_STATE)? msg.getState().getText() : "<stateless>"
    }

    /**
     * Extract the ticker (RIC) from the given message.
     */
    private static String getTicker(OMMMsg msg) {
        assert msg.has(OMMMsg.HAS_ATTRIB_INFO)

        return msg.getAttribInfo().getName()
    }

    /**
     * Conditionally parse and dump the entire payload to the log.
     */
    private void tracePayload(OMMData payload) {
        if (log.isTraceEnabled()) {
            Map<String, Double> map = new HashMap<>()
            ommPayloadParser.parsePayload(payload, map)
            log.trace(map.toString())
        }
    }

    /**
     * Parse the event; if it contains market data, pass it on to our sink.
     */
    @Override
    void processEvent(Event event) {
        assert event, "event cannot be null"

        if (event.getType() == Event.COMPLETION_EVENT) {
            // Completion event indicates that the stream was closed by RFA
            log.info("Stream was closed by RFA: {}", event.toString())

        } else if (event.getType() == Event.OMM_ITEM_EVENT) {
            OMMItemEvent itemEvent = (OMMItemEvent) event
            OMMMsg responseMsg = itemEvent.msg
            log.trace(
                    "Received event type {} with state {} from handle {}",
                    event.type, getStateText(responseMsg), event.handle)

            if (responseMsg.getDataType() != OMMTypes.NO_DATA) {
                OMMData payload = responseMsg.getPayload()
                assert payload != null
                tracePayload(payload)
                String ticker = getTicker(responseMsg)
                double value = ommPayloadParser.parsePayload(payload)
                if (!Double.isNaN(value)) {
                    sink.accept(new Quote(ticker, value))
                } else {
                    log.trace("Missing traded price in event for {}", ticker)
                }
            }
        } else {
            log.error("Received unsupported Event type {} from {}", event.type, event.handle)
        }
    }
}
