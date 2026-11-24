#!/usr/bin/env sh
set -eu
: "${CMS_CLIENT_SECRET:?Set CMS_CLIENT_SECRET before running}"
exec java -jar "$(dirname "$0")/target/cms-cli-consumer-1.0.0.jar"
