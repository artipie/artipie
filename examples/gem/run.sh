#!/bin/bash

set -e
set -x

# Push a gem into artipie.
export GEM_HOST_API_KEY=$(echo -n "hello:world" | base64)
cd /test/sample-project
gem build sample-project.gemspec
gem push sample-project-1.0.0.gem --host http://artipie.artipie:8080/my-gem
cd ..

# Fetch the uploaded earlier gem from artipie.
gem fetch sample-project --source http://artipie.artipie:8080/my-gem
