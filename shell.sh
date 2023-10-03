#!/bin/bash

set -e

FILE=spring-data-cockroachdb-shell/target/spring-data-cockroachdb-shell.jar

if [ ! -f "$FILE" ]; then
    ./mvnw clean install
fi

java -jar $FILE "$@"
