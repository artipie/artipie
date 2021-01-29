#!/bin/bash
set -x
set -e

# Upload a maven project
mvn -B --quiet -f sample-for-deployment deploy -Dmaven.install.skip=true

# Upload the sample-for-deployment
mvn -B --quiet -f sample-consumer -U clean install
