set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Upload a maven project
mvn -f sample-for-deployment deploy -Dmaven.install.skip=true

# Upload the sample-for-deployment
mvn -f sample-consumer -U clean install

# Remove container.
docker stop artipie
