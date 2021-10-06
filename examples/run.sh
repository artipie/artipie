#!/bin/bash
set -e

basedir="$(dirname $(readlink -f $0))"

# environment variables:
#  - ARTIPIE_NOSTOP - don't stop docker containers
#       don't remove docker network on finish
#  - DEBUG - show debug messages
#  - CI - enable CI mode (debug and `set -x`)
#  - DEBUG_NOX - don't enable `set -x` mode
#  - ARTIPIE_IMAGE - docker image name for artipie
#       (default artipie/artipie:1.0-SNAPSHOT)

# print error message and exist with error code
function die {
  printf "FATAL: %s\n" "$1"
  exit 1
}

# check if environment variable is present or die
function require_env {
  local name="$1"
  local val=$(eval "echo \${$name}")
  if [[ -z "$val" ]]; then
    die "${name} env should be set"
  fi
}

require_env basedir

# set debug on CI builds
if [[ -n "$CI" ]]; then
  export DEBUG=true
fi

# print debug message if DEBUG mode enabled
function log_debug {
  if [[ -n "$DEBUG" ]]; then
    printf "DEBUG: %s\n" "$1"
  fi
}

# check if first param is equal to second or die
function assert {
  [[ "$1" -ne "$2" ]] && die "assertion failed: ${1} != ${2}"
}

if [[ -n "$DEBUG" ]]; then
  [[ -z "$DEBUG_NOX" ]] && set -x
  log_debug "debug enabled"
fi

# start artipie docker image. image name and port are optional
# parameters. register callback to stop image on exist.
function start_artipie {
  local image="$1"
  if [[ -z "$image" ]]; then
    image=$ARTIPIE_IMAGE
  fi
  if [[ -z "$image" ]]; then
    image="artipie/artipie:1.0-SNAPSHOT"
  fi
  local port="$2"
  if [[ -z "$port" ]]; then
    port=8080
  fi
  log_debug "using image: '${image}'"
  log_debug "using port:  '${port}'"
  [[ -z "$image" || -z "$port" ]] && die "invalid image or port params"
  stop_artipie
  docker run --rm --detach --name artipie \
    -v "${basedir}/artipie.yml:/etc/artipie/artipie.yml" \
    -v "${basedir}/.cfg:/var/artipie/cfg" \
    --mount source=artipie-data,destination=/var/artipie/data \
    --user 2020:2021 \
    --net=artipie \
    -p "${port}:8080" "$image"
  log_debug "artipie started"
  # stop artipie docker container on script exit
  if [[ -z "$ARTIPIE_NOSTOP" ]]; then
    trap stop_artipie EXIT
  fi
}

function stop_artipie {
  local container=$(docker ps --filter name=artipie -q 2> /dev/null)
  if [[ -n "$container" ]]; then
    log_debug "stopping artipie container ${container}"
    docker stop "$container" || echo "failed to stop"
  fi
}

# create docker network named `artipie` for containers communication
# register callback to remove it on script exit if no ARTIPIE_NOSTOP
# environment is set
function create_network {
  rm_network
  log_debug "creating artipie network"
  docker network create artipie
  if [[ -z "$ARTIPIE_NOSTOP" ]]; then
    trap rm_network EXIT
  fi
}

# remove `artipie` network if exist
function rm_network {
  local net=$(docker network ls -q --filter name=artipie)
  if [[ -n "${net}" ]]; then
    log_debug "removing artipie network"
    docker network rm $net
  fi
}

function create_volume {
  rm_volume
  log_debug "creating volume $(docker volume create artipie-data)"
  log_debug "fill out volume data"
  docker run --rm --name=artipie-volume-maker \
    -v ${basedir}/.data:/data-src \
    --mount source=artipie-data,destination=/data-dst \
    alpine:3.13 \
    /bin/sh -c 'addgroup -S -g 2020 artipie && adduser -S -g 2020 -u 2021 artipie && cp -r /data-src/* /data-dst && chown -R 2020:2021 /data-dst'
  if [[ -z "$ARTIPIE_NOSTOP" ]]; then
    trap rm_volume EXIT
  fi
}

# remove artipie data volume if exist
function rm_volume {
  local img=$(docker volume ls -q --filter name=artipie-data)
  if [[ -n "${img}" ]]; then
    log_debug "removing volume "
    docker volume rm ${img}
  fi
}

# run single smoke-test
function run_test {
  local name=$1
  log_debug "running smoke test $name"
  cd "${basedir}/${name}"
  docker build -t "test/${name}" .
  docker run --name="smoke-${name}" --rm \
    --net=artipie \
    -v /var/run/docker.sock:/var/run/docker.sock \
    "test/${name}" | tee -a "${basedir}/out.log"
  if [[ "${PIPESTATUS[0]}" == "0" ]]; then
    echo "test ${name} - PASSED" | tee -a "${basedir}/results.txt"
  else
    echo "test ${name} - FAILED" | tee -a "${basedir}/results.txt"
  fi
}

create_network
create_volume
start_artipie

if [[ -z "$1" ]]; then
  declare -a tests=(binary debian docker go helm maven npm nuget php rpm conda)
else
  declare -a tests=("$1")
fi

log_debug "tests: ${tests[@]}"

# FIXME: some repository tests don't work, fix them:
#     go nuget

rm -fr "${basedir}/out.log" "${basedir}/results.txt"
touch "${basedir}/out.log"

for t in "${tests[@]}"; do
  run_test $t || echo "test $t failed"
done

echo "all tests finished:"
cat "${basedir}/results.txt"
grep "FAILED" "${basedir}/results.txt" && die "One or more tests failed"
