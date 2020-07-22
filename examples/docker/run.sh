set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Pull an image from docker hub.
docker pull ubuntu

# Push the pulled image to artipie
docker image tag ubuntu localhost:8080/my-docker/myfirstimage
docker push localhost:8080/my-docker/myfirstimage

# Remove container.
docker stop artipie