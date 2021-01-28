set -x
set -e

image="$1"
if [[ -z "$image" ]]; then
  image="artipie/artipie:latest"
fi
basedir="$(dirname $(readlink -f $0))"

# Start artipie.
docker run --rm --detach --name artipie \
  -v "${basedir}/artipie.yaml:/etc/artipie/artipie.yml" \
  -v "${basedir}:/var/artipie" \
  -p 8080:80 "$image"

# Publish the sample-npm-project.
cd "${basedir}/sample-npm-project"
npm publish --registry http://localhost:8080/npm_repo/

# Install the sample-npm-project.
cd "${basedir}/sample-consumer"
npm install sample-npm-project --registry http://localhost:8080/npm_repo/

# Stop and remove container.
docker stop artipie
