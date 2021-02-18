#!/bin/bash

set -e
set -x

# Post a package.
curl -i  -X PUT  --data-binary "@aglfn_1.7-3_amd64.deb" http://artipie.artipie:8080/my-debian/main/aglfn_1.7-3_amd64.deb

# Update the world and install posted package.
apt-get update
apt-get install -y aglfn
