#!/bin/bash
set -x
set -e

# Upload tar via curl.
cd sample-for-deployment
curl -X POST --data-binary "@decimal-2.0.0.tar" http://artipie.artipie:8080/my-hexpm/publish?replace=false

# Install mix
cd ../sample-consumer/kv
mix local.hex --force

# Add ref to Artipie repository.
mix hex.repo add my_repo http://artipie.artipie:8080/my-hexpm

# Fetch the uploaded tar.
mix hex.package fetch decimal 2.0.0 --repo=my_repo
