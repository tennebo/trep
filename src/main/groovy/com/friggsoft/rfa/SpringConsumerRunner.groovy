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

        // Get RICs to subscribe to from the commandline
        String[] rics
        if (args.containsOption("ric")) {
            rics = args.getOptionValues("ric")
        } else {
            log.error("Please provide a RIC to listen to, e.g. --ric=TRI.N")
            System.exit(1)
        }

        try {
            log.info("Starting consumer {}", consumer)
            consumer.run(rics)
        } finally {
            log.info("Shutting down consumer {}", consumer)
            consumer.close()
        }
    }
}
