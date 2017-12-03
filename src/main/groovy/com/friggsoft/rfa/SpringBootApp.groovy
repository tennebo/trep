package com.friggsoft.rfa

import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

import ch.qos.logback.classic.LoggerContext
import groovy.util.logging.Slf4j

/**
 * Spring Boot app to run either the provider, consumer or both..
 */
@Slf4j
@SpringBootApplication
class SpringBootApp {

    static void main(String... args) {
        // Handle SIGTERM and SIGINT (Control+C)
        Thread shutterDowner = new Thread({ jvmShutdownHook() },"shutdown-hook")
        Runtime.getRuntime().addShutdownHook(shutterDowner)

        SpringApplication.run SpringBootApp, args
    }

    /**
     * No need to close the Spring context; Spring Boot has registered its own shutdown hook.
     * @see org.springframework.context.ConfigurableApplicationContext#registerShutdownHook()
     */
    private static void jvmShutdownHook() {
        log.info("Running JVM shutdown hook")
        shutdown()
    }

    /**
     * Shutdown services that are not closed by Spring.
     */
    private static void shutdown() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory()
        if (loggerFactory instanceof LoggerContext) {
            log.info("Shutting down Logback")
            LoggerContext loggerContext = (LoggerContext) loggerFactory
            loggerContext.stop()
        }
    }
}
