#!/bin/sh
#
# Execute the Spring Boot consumer app.

dir=`dirname $0`
${dir}/run.sh --spring.profiles.active=consumer

