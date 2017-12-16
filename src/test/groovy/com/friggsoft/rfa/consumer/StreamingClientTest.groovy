package com.friggsoft.rfa.consumer

import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify

import java.util.function.Consumer

import org.junit.Test
import org.mockito.Mockito

import com.reuters.rfa.common.Event
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMTypes
import com.reuters.rfa.session.omm.OMMItemEvent

class StreamingClientTest {

    @Test
    void testCompletionEvent() {
        Event event = Mockito.mock(Event.class)
        Mockito.when(event.getType()).thenReturn(Event.COMPLETION_EVENT)

        Consumer<Quote> sink = Mockito.mock(Consumer.class)
        StreamingClient client = new StreamingClient(sink)

        // This should result in no calls to the sink
        client.processEvent(event)

        verify(sink, times(0)).accept(Mockito.<Quote>any())
    }

    @Test
    void testBadEventType() {
        Event event = Mockito.mock(Event.class)
        Mockito.when(event.getType()).thenReturn(Event.UNDEFINED_EVENT)

        Consumer<Quote> sink = Mockito.mock(Consumer.class)
        StreamingClient client = new StreamingClient(sink)

        // This should result in a warning, but no exception
        client.processEvent(event)

        verify(sink, times(0)).accept(Mockito.<Quote>any())
    }

    @Test
    void testItemEventNoData() {
        OMMMsg ommMsg = Mockito.mock(OMMMsg.class)
        Mockito.when(ommMsg.getState()).thenReturn(null)
        Mockito.when(ommMsg.getDataType()).thenReturn(OMMTypes.NO_DATA)

        OMMItemEvent event = Mockito.mock(OMMItemEvent.class)
        Mockito.when(event.getType()).thenReturn(Event.OMM_ITEM_EVENT)
        Mockito.when(event.getMsg()).thenReturn(ommMsg)

        Consumer<Quote>sink = Mockito.mock(Consumer.class)
        StreamingClient client = new StreamingClient(sink)

        // This should result in no calls to the sink
        client.processEvent(event)

        verify(sink, times(0)).accept(Mockito.<Quote>any())
    }
}
