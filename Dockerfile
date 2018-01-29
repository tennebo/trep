# Alpine Linux with Java 8
FROM openjdk:8-jdk-alpine

# Add our Spring Boot executable
COPY ./build/libs/trep*.jar the.jar

EXPOSE 14002

# Spring Boot profiles
ENV SPRING_PROFILES_ACTIVE=provider
ENV JAVA_OPTS=""

ENTRYPOINT exec java $JAVA_OPTS -jar the.jar
