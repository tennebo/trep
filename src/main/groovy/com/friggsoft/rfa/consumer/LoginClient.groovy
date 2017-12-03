package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import com.friggsoft.rfa.config.Constants
import com.reuters.rfa.common.Client
import com.reuters.rfa.common.Event
import com.reuters.rfa.common.EventQueue
import com.reuters.rfa.common.Handle
import com.reuters.rfa.config.ConfigProvider
import com.reuters.rfa.omm.OMMElementList
import com.reuters.rfa.omm.OMMEncoder
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.omm.OMMState
import com.reuters.rfa.omm.OMMTypes
import com.reuters.rfa.rdm.RDMMsgTypes
import com.reuters.rfa.rdm.RDMUser
import com.reuters.rfa.session.omm.OMMConsumer
import com.reuters.rfa.session.omm.OMMItemEvent
import com.reuters.rfa.session.omm.OMMItemIntSpec

/**
 * Handle login activities between application and RFA.
 */
@Slf4j
final class LoginClient implements Client {

    private volatile boolean loggedIn = false

    /** Latch to enable synchronous login requests. */
    private final CountDownLatch latch = new CountDownLatch(1)

    /**
     * Return true if the login request has completed (whether successful or failed).
     */
    boolean isDone() {
        return latch.getCount() == 0
    }

    /**
     * Return true if we have successfully logged in.
     */
    boolean isLoggedIn() {
        return isDone() && loggedIn
    }

    /**
     * Block the calling thread until the login request has succeeded or failed.
     */
    boolean awaitLogin(long timeout, TimeUnit unit) {
        try {
            log.info("Awaiting login response for {} {}...", timeout, unit)
            boolean zero = latch.await(timeout, unit)
            if (!zero) {
                log.warn("Login timed out")
            } else {
                log.info("Received login response: {}", (isLoggedIn()? "Login succeeded" : "Login failed"))
            }
        } catch (InterruptedException e) {
            // Treat this as login failed
            log.warn("Interrupted while awaiting login clearing interrupt flag")
            Thread.currentThread().interrupt()
        }
        return isLoggedIn()
    }

    /**
     * Get encoded request message for the login and register this client with RFA.
     */
    Handle sendLoginRequestAsync(
            ConfigProvider configDb, EventQueue eventQueue,
            OMMConsumer ommConsumer, OMMPool ommPool) {
        OMMEncoder encoder = ommPool.acquireEncoder()
        try {
            encoder.initialize(OMMTypes.MSG, 500)
            OMMMsg ommMsg = encodeLoginRequestMsg(configDb, ommPool, encoder)
            OMMItemIntSpec ommItemIntSpec = new OMMItemIntSpec()
            ommItemIntSpec.setMsg(ommMsg)

            log.info("Sending login request")
            return ommConsumer.registerClient(eventQueue, ommItemIntSpec, this, null)
        } finally {
            ommPool.releaseEncoder(encoder)
        }
    }

    /**
     * Encode the login request message.
     */
    private static OMMMsg encodeLoginRequestMsg(ConfigProvider configDb, OMMPool ommPool, OMMEncoder encoder) {
        String application = configDb.variable("", Constants.application)
        String username = configDb.variable("", Constants.user)
        String position = configDb.variable("", Constants.position)

        OMMMsg msg = ommPool.acquireMsg()
        try {
            msg.setMsgType(OMMMsg.MsgType.REQUEST)
            msg.setMsgModelType(RDMMsgTypes.LOGIN)
            msg.setIndicationFlags(OMMMsg.Indication.REFRESH)
            msg.setAttribInfo(null, username, RDMUser.NameType.USER_NAME)

            encoder.encodeMsgInit(msg, OMMTypes.ELEMENT_LIST, OMMTypes.NO_DATA)
            encoder.encodeElementListInit(OMMElementList.HAS_STANDARD_DATA, (short) 0, (short) 0)
            encoder.encodeElementEntryInit(RDMUser.Attrib.ApplicationId, OMMTypes.ASCII_STRING)
            encoder.encodeString(application, OMMTypes.ASCII_STRING)
            encoder.encodeElementEntryInit(RDMUser.Attrib.Position, OMMTypes.ASCII_STRING)
            encoder.encodeString(position, OMMTypes.ASCII_STRING)
            encoder.encodeElementEntryInit(RDMUser.Attrib.Role, OMMTypes.UINT)
            encoder.encodeUInt((long) RDMUser.Role.CONSUMER)
            encoder.encodeAggregateComplete()

            // Get the encoded message from the encoder
            return (OMMMsg) encoder.getEncodedObject()
        } finally {
            ommPool.releaseMsg(msg)
        }
    }

    @Override
    void processEvent(Event event) {
        switch (event.getType()) {
            case Event.COMPLETION_EVENT:
                // Completion event indicates that the stream was closed by RFA
                log.info("Received COMPLETION_EVENT: {}", event.getHandle())
                break
            case Event.OMM_ITEM_EVENT:
                OMMItemEvent itemEvent = (OMMItemEvent) event
                OMMMsg responseMsg = itemEvent.getMsg()
                assert responseMsg.getMsgModelType() == RDMMsgTypes.LOGIN
                if (responseMsg.isFinal()) {
                    logMessage(responseMsg)
                    onLoginFailure()
                } else {
                    // When the login is successful, RFA forwards the message from the network
                    if ((responseMsg.getMsgType() == OMMMsg.MsgType.STATUS_RESP) &&
                            (responseMsg.has(OMMMsg.HAS_STATE)) &&
                            (responseMsg.getState().getStreamState() == OMMState.Stream.OPEN) &&
                            (responseMsg.getState().getDataState() == OMMState.Data.OK)) {
                        logMessage(responseMsg)
                        onLoginSuccess()
                    } else {
                        // RFA is processing the login
                        log.info("Login is being processed: {}", OMMMsg.MsgType.toString(responseMsg.getMsgType()))
                        logMessage(responseMsg)
                        assert responseMsg.getMsgType() == OMMMsg.MsgType.REFRESH_RESP
                    }
                }
                break
            default:
                log.error("Unhandled event type: {}", event.getType())
                break
        }
    }

    /** Called upon receiving successful login response. */
    private void onLoginSuccess() {
        log.info("Login successful")
        loggedIn = true
        latch.countDown()
    }

    /** Called upon login failure. */
    private void onLoginFailure() {
        log.error("Login failed")
        loggedIn = false
        latch.countDown()
    }

    /** For diagnostics only. */
    private static void logMessage(OMMMsg msg) {
        log.info("Msg Type: " + OMMMsg.MsgType.toString(msg.getMsgType()))
        log.info("Msg Model Type: " + RDMMsgTypes.toString(msg.getMsgModelType()))
        log.info("Indication Flags: " + OMMMsg.Indication.indicationString(msg))
    }
}
