package com.friggsoft.rfa

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import com.friggsoft.rfa.config.ProviderProperties
import com.friggsoft.rfa.provider.ProviderApp
import com.reuters.rfa.config.ConfigDb

import groovy.util.logging.Slf4j

/**
 * Spring Boot runner for the streaming provider (producer) app.
 */
@Slf4j
@Profile(ProviderProperties.PROFILE_NAME)
@Component
class SpringProviderRunner implements ApplicationRunner {

    /**
     * Database of RFA configuration parameters.
     */
    private final ConfigDb configDb

    SpringProviderRunner(ConfigDb configDb) {
        this.configDb = configDb
    }

    @Override
    void run(ApplicationArguments args) {
        ProviderApp provider = new ProviderApp(configDb)
        try {
            log.info("Starting provider {}", provider)
            provider.dispatchEvents()
        } finally {
            log.info("Shutting down provider {}", provider)
            provider.close()
        }
    }
}
