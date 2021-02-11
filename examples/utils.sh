set -e

function die {
  printf "FATAL: %s\n" "$1"
  exit 1
}

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

function log_debug {
  if [[ -n "$DEBUG" ]]; then
    printf "DEBUG: %s\n" "$1"
  fi
}

function assert {
  [[ "$1" -ne "$2" ]] && die "assertion failed: ${1} != ${2}"
}

if [[ -n "$DEBUG" ]]; then
  [[ -z "$DEBUG_NOX" ]] && set -x
  log_debug "debug enabled"
fi

function start_artipie {
  local image="$1"
  if [[ -z "$image" ]]; then
    image=$ARTIPIE_IMAGE
  fi
  if [[ -z "$image" ]]; then
    image="artipie/artipie:latest"
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
    -v "${basedir}/artipie.yaml:/etc/artipie/artipie.yml" \
    -v "${basedir}:/var/artipie" \
    -p "${port}:80" "$image"
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
    docker stop "$container"
  fi
}

