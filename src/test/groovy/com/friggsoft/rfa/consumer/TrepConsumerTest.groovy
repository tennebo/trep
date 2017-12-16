package com.friggsoft.rfa.consumer

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import com.reuters.rfa.config.ConfigProvider

/**
 * Unit test of class {@link TrepConsumer}.
 */
class TrepConsumerTest {
    private TrepConsumer trepConsumer

    @Before
    void setUp() {
        ConfigProvider configProvider = ConfigProviderFactory.newConfigProvider()
        trepConsumer = new TrepConsumer(configProvider)
    }

    @After
    void tearDown() {
        trepConsumer.close()
    }

    @Test
    void testForgotToLogIn() {
        try {
            String[] rics = new String[0]
            trepConsumer.subscribe(rics)
            Assert.fail("Subscribing without login in first should fail")
        } catch (ignored) {
            /* Good */
        }
    }
}
