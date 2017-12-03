#!/bin/sh
#
# Execute the Spring Boot provider app.

dir=`dirname $0`
${dir}/run.sh --spring.profiles.active=provider

