#!/bin/bash

set -e
set -x

# Force go client to use Aritpie Go registry.
export GO111MODULE=on
export GOPROXY=http://artipie.artipie:8080/my-go

# Install from Artipie Go registry.
go get -x -insecure golang.org/x/time
