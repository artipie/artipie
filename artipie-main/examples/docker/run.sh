#!/bin/bash

set -e
set -x

# Pull an image from docker hub.
docker pull ubuntu

# Login to artipie.
docker login --username alice --password qwerty123 http://localhost:8080

img="localhost:8080/my-docker/myfirstimage"
# Push the pulled image to artipie.
docker image tag ubuntu $img
docker push $img

# Pull the pushed image from artipie.
docker image rm $img
docker pull $img
