set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Publish the sample-npm-project.
cd sample-npm-project
npm publish --registry http://localhost:8080/npm_repo/
cd -

# Install the sample-npm-project.
cd sample-consumer
npm install sample-npm-project --registry http://localhost:8080/npm_repo/
cd -

# Remove container.
docker stop artipie