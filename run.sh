#!/bin/sh
#
# Execute the Spring Boot app.

# Spring Boot executable jar
jarname=trep-test-0.2.0-SNAPSHOT.jar

# Location of this script
script_home=`dirname $0`

# Gradle output locations
target=${script_home}/build
libdir=${target}/libs
config=${target}/resources/main/

# Spring Boot executable jar
jar=${libdir}/${jarname}

java -Dspring.config.location=file:${config} -jar ${jar} $*

