package com.friggsoft.rfa.consumer

import java.util.concurrent.TimeUnit

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import com.reuters.rfa.common.Event
import com.reuters.rfa.config.ConfigProvider
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMState
import com.reuters.rfa.omm.OMMTypes
import com.reuters.rfa.rdm.RDMMsgTypes
import com.reuters.rfa.session.omm.OMMItemEvent

class LoginClientTest {
    private TrepConnector trepConnector

    @Before
    void setUp() {
        ConfigProvider configProvider = ConfigProviderFactory.newConfigProvider()
        trepConnector = new TrepConnector(configProvider)
    }

    @After
    void tearDown() {
        trepConnector.close()
    }

    @Test
    void testNotLoggedIn() {
        LoginClient loginClient = new LoginClient()

        Assert.assertFalse(loginClient.isDone())
        Assert.assertFalse(loginClient.isLoggedIn())
        Assert.assertFalse(loginClient.awaitLogin(0, TimeUnit.MILLISECONDS))
    }

    @Test
    void testNotLoggedInAsync() {
        LoginClient loginClient = new LoginClient()

        // There is no server to connect to, so this will fail
        loginClient.sendLoginRequestAsync(
                trepConnector.getConfigProvider(),
                trepConnector.getEventQueue(),
                trepConnector.getOmmConsumer(),
                trepConnector.getOmmPool())

        Assert.assertFalse(loginClient.isDone())
        Assert.assertFalse(loginClient.isLoggedIn())
        Assert.assertFalse(loginClient.awaitLogin(0, TimeUnit.MILLISECONDS))
    }

    @Test
    void testCompletionEvent() {
        Event event = Mockito.mock(Event.class)
        Mockito.when(event.getType()).thenReturn(Event.COMPLETION_EVENT)

        LoginClient loginClient = new LoginClient()

        // This should be ignored
        loginClient.processEvent(event)

        Assert.assertFalse(loginClient.isDone())
        Assert.assertFalse(loginClient.isLoggedIn())
    }

    @Test
    void testBadEventType() {
        Event event = Mockito.mock(Event.class)
        Mockito.when(event.getType()).thenReturn(Event.UNDEFINED_EVENT)

        LoginClient loginClient = new LoginClient()

        // This should result in a warning, but no exception
        loginClient.processEvent(event)

        Assert.assertFalse(loginClient.isDone())
        Assert.assertFalse(loginClient.isLoggedIn())
    }

    @Test
    void testItemEventLoginFailed() {
        OMMItemEvent event = makeMockLoginEvent(false)

        LoginClient loginClient = new LoginClient()

        // This should result in a failed login
        loginClient.processEvent(event)
        loginClient.awaitLogin(0, TimeUnit.MICROSECONDS) // Should return immediately

        Assert.assertTrue(loginClient.isDone())
        Assert.assertFalse(loginClient.isLoggedIn())
    }

    @Test
    void testItemEventLoginInProgress() {
        OMMMsg ommMsg = Mockito.mock(OMMMsg.class)
        Mockito.when(ommMsg.getMsgModelType()).thenReturn(RDMMsgTypes.LOGIN)
        Mockito.when(ommMsg.isFinal()).thenReturn(false)
        Mockito.when(ommMsg.getMsgType()).thenReturn(OMMMsg.MsgType.REFRESH_RESP)
        Mockito.when(ommMsg.has(OMMMsg.HAS_STATE)).thenReturn(false)

        OMMItemEvent event = Mockito.mock(OMMItemEvent.class)
        Mockito.when(event.getType()).thenReturn(Event.OMM_ITEM_EVENT)
        Mockito.when(event.getMsg()).thenReturn(ommMsg)

        LoginClient loginClient = new LoginClient()

        // This should result in login being processed
        loginClient.processEvent(event)

        Assert.assertFalse(loginClient.isDone())
        Assert.assertFalse(loginClient.isLoggedIn())
    }

    @Test
    void testItemEventLoginSucceeded() {
        OMMItemEvent event = makeMockLoginEvent(true)

        LoginClient loginClient = new LoginClient()

        // This should result in a successful login
        loginClient.processEvent(event)
        loginClient.awaitLogin(0, TimeUnit.MICROSECONDS) // Should return immediately

        Assert.assertTrue(loginClient.isDone())
        Assert.assertTrue(loginClient.isLoggedIn())
    }

    /**
     * Create and return a mocked 'login succeeded' or 'login failed' OMM event.
     */
    private static OMMItemEvent makeMockLoginEvent(boolean success) {
        OMMState ommState = Mockito.mock(OMMState.class)
        Mockito.when(ommState.getStreamState()).thenReturn(success? OMMState.Stream.OPEN : OMMState.Stream.CLOSED)
        Mockito.when(ommState.getDataState()).thenReturn(OMMState.Data.OK)

        OMMMsg ommMsg = Mockito.mock(OMMMsg.class)
        Mockito.when(ommMsg.getMsgModelType()).thenReturn(RDMMsgTypes.LOGIN)
        Mockito.when(ommMsg.isFinal()).thenReturn(!success)
        Mockito.when(ommMsg.getMsgType()).thenReturn(OMMMsg.MsgType.STATUS_RESP)
        Mockito.when(ommMsg.has(OMMMsg.HAS_STATE)).thenReturn(true)
        Mockito.when(ommMsg.getState()).thenReturn(ommState)
        Mockito.when(ommMsg.getDataType()).thenReturn(OMMTypes.NO_DATA)

        OMMItemEvent event = Mockito.mock(OMMItemEvent.class)
        Mockito.when(event.getType()).thenReturn(Event.OMM_ITEM_EVENT)
        Mockito.when(event.getMsg()).thenReturn(ommMsg)

        return event
    }
}