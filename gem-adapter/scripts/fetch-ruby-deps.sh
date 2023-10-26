#!/bin/bash

# The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
# https://github.com/artipie/artipie/LICENSE.txt

# This script downloads required runtime ruby dependencies for
# Gem API. It will be fetched and installed into `./lib` dir
# and then packed into jar file with Maven. These libs will be
# accessible as resources in classpath, JRuby will load it
# automatically on `require` statement.

set -e

# die - print message and exit with error code
function die {
  echo "ERROR: $@"
  exit 1
}

# check_cmd - check if all commands from array are present
function check_cmd {
  for cmd in "$@"; do
    command -v "$cmd" > /dev/null || die "$cmd command is not found"
  done
}

# check if gem and ruby command exist
check_cmd gem ruby

# target directory for ruby libs
libdir="$PWD/lib"

# fetch and install gem locally
function gem_install_local {
  for name in "$@"; do
    echo "installing $name"
    gem fetch $name
    local name=$name
    local gem=$(find . -type f -regex "\./${name}.+\.gem")
    env GEM_PATH="$libdir" GEM_HOME="$libdir" \
      ruby -S gem install "$gem" --no-document
    rm -v "$gem"
  done
}

mkdir -p "$libdir"

# install builder gem to local lib dir
gem_install_local builder

echo "done: installed successfully"
