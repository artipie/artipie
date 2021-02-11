basedir="$(dirname $(readlink -f $0))"
source "${basedir}/../utils.sh"

image="$1"
port=8080

opts="--registry=http://localhost:${port}/npm_repo"

start_artipie "$image" "$port"
cd "${basedir}/sample-npm-project" && npm publish "$opts"
cd "${basedir}/sample-consumer" && npm install "$opts"
cd "${basedir}/sample-npm-project" && npm unpublish "$opts" "sample-npm-project@1.0.0"
