#!/bin/bash

set -e
set -x

# Download original package
conan install zlib/1.2.13@ -r conancenter --build=missing

# Upload package.
conan upload zlib/1.2.13@ -r conan-test --all

# Clear cache
rm -rfv $HOME/.conan/data

# Install the package.
conan install zlib/1.2.13@ -r conan-test || ret=$?
cat /tmp/conan_trace.log
exit $ret
