package com.friggsoft.rfa

import groovy.util.logging.Slf4j

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import com.friggsoft.rfa.config.ConsumerProperties
import com.friggsoft.rfa.consumer.ConsumerApp
import com.reuters.rfa.config.ConfigDb

/**
 * Spring Boot runner for the streaming consumer app.
 */
@Slf4j
@Profile(ConsumerProperties.PROFILE_NAME)
@Component
class SpringConsumerRunner implements ApplicationRunner {

    /**
     * Database of RFA configuration parameters.
     */
    private final ConfigDb configDb

    SpringConsumerRunner(ConfigDb configDb) {
        this.configDb = configDb
    }

    @Override
    void run(ApplicationArguments args) {
        ConsumerApp consumer = new ConsumerApp(configDb)
        try {
            log.info("Starting consumer {}", consumer)
            consumer.run()
        } finally {
            log.info("Shutting down consumer {}", consumer)
            consumer.close()
        }
    }
}
