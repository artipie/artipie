#!/bin/bash

set -e
set -x

# Create a file for subsequent publication.
echo "hello world" > text.txt

curl -X PUT --data-binary "@text.txt" http://artipie.artipie:8080/bin/text.txt

# Download the file.
STATUSCODE=$(curl --silent --output /dev/stderr --write-out "%{http_code}" http://artipie.artipie:8080/bin/text.txt)

# Make sure status code is 200.
if [[ "$STATUSCODE" -ne 200 ]]; then
  echo "TEST_FAILURE: binary response status=$STATUSCODE"
  exit 1
else
  echo "binary test completed succesfully"
fi
