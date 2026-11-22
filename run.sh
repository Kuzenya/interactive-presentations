#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ ! -f target/interactive-presentations-1.0.0.jar ]; then
    mvn package
fi
java -Dfile.encoding=UTF-8 -jar target/interactive-presentations-1.0.0.jar "$@"
