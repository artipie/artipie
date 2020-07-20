set -x
set -e

# Start artipie.
docker run -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Upload a maven project
cd sample-for-deployment
mvn clean deploy -Dmaven.install.skip=true
cd ..

cd sample-consumer
mvn -U clean install
cd ..


# Remove container.
docker rm -f artipie