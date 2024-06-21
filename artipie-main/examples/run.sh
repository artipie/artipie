#!/bin/bash
set -eo pipefail
cd ${0%/*}
echo "running in $PWD"
workdir=$PWD

# environment variables:
#  - ARTIPIE_NOSTOP - don't stop docker containers
#       don't remove docker network on finish
#  - DEBUG - show debug messages
#  - CI - enable CI mode (debug and `set -x`)
#  - ARTIPIE_IMAGE - docker image name for artipie
#       (default artipie/artipie-tests:1.0-SNAPSHOT)

# print error message and exist with error code
function die {
  printf "FATAL: %s\n" "$1"
  exit 1
}

# set pidfile to prevent parallel runs
pidfile=/tmp/test-artipie.pid
if [[ -f $pidfile ]]; then
  pid=$(cat $pidfile)
  set +e
  ps -p $pid > /dev/null 2>&1
  [[ $? -eq 0 ]] || die "script is already running"
  set -e
fi
echo $$ > $pidfile
trap "rm -v $pidfile" EXIT

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
    image="artipie/artipie-tests:1.0-SNAPSHOT"
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
    -v "$PWD/artipie.yml:/etc/artipie/artipie.yml" \
    -v "$PWD/.cfg:/var/artipie/cfg" \
    -e ARTIPIE_USER_NAME=alice \
    -e ARTIPIE_USER_PASS=qwerty123 \
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
    -v "$PWD/.data:/data-src" \
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
  pushd "./${name}"
  docker build -t "test/${name}" .
  docker run --name="smoke-${name}" --rm \
    --net=artipie \
    -v /var/run/docker.sock:/var/run/docker.sock \
    "test/${name}" | tee -a "$workdir/out.log"
  if [[ "${PIPESTATUS[0]}" == "0" ]]; then
    echo "test ${name} - PASSED" | tee -a "$workdir/results.txt"
  else
    echo "test ${name} - FAILED" | tee -a "$workdir/results.txt"
  fi
  popd
}

create_network
create_volume
start_artipie

sleep 3 #sometimes artipie container needs extra time to load

if [[ -z "$1" ]]; then
#TODO: hexpm is removed from the list due to the issue: https://github.com/artipie/artipie/issues/1464
  declare -a tests=(binary debian docker go helm maven npm nuget php rpm conda pypi conan)
else
  declare -a tests=("$@")
fi

log_debug "tests: ${tests[@]}"

rm -fr "$workdir/out.log" "$workdir/results.txt"
touch "$workdir/out.log"

for t in "${tests[@]}"; do
  run_test $t || echo "test $t failed"
done

echo "all tests finished:"
cat "$workdir/results.txt"
r=0
grep "FAILED" "$workdir/results.txt" > /dev/null || r="$?"
if [ "$r" -eq 0 ] ; then
  rm -fv "$pidfile"
  echo "Artipie container logs:"
  container=$(docker ps --filter name=artipie -q 2> /dev/null)
  if [[ -n "$container" ]] ; then
    docker logs "$container" || echo "failed to log artipie"
  fi
  die "One or more tests failed"
else
  rm -fv "$pidfile"
  echo "SUCCESS"
fi

