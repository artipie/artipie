#!/bin/bash

opts="--registry=http://artipie.artipie:8080/npm_repo"

cd /test/sample-npm-project && npm publish "$opts"
cd /test/sample-consumer && npm install "$opts"
cd /test/sample-npm-project && npm unpublish "$opts" "sample-npm-project@1.0.0"
