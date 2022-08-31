#!/bin/bash
set -x
set -e

# Upload tar via curl.
curl -X POST --data-binary "@sample-for-deployment/decimal-2.0.0.tar" http://artipie.artipie:8080/hexpm/publish?replace=false

# Install mix
cd sample-consumer/kv
mix local.hex --force

# Add ref to Artipie repository.
mix hex.repo add my_repo http://artipie.artipie:8080/hexpm

# Fetch the uploaded tar.
mix hex.package fetch decimal 2.0.0 --repo=my_repo
