// Log config for logback

appender('console', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss} %highlight(%-5level) %cyan([%-9thread]) %logger{20} - %msg%n"
    }
     immediateFlush = false
}

// Set logging levels
logger("com.friggsoft", INFO)
logger("com.friggsoft.rfa.consumer.StreamingClient", TRACE)
logger("org.springframework", WARN)
logger("org.springframework.boot.SpringApplication", WARN)

// Reuters RFA
logger("com.reuters", INFO)
logger("com.thomsonreuters", INFO)

// Setup a console logger
root(INFO, ['console'])
