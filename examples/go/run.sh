set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Force go client to use Aritpie Go registry.
export GO111MODULE=on
export GOPROXY=http://localhost:8080/my-go

# Install from Artipie Go registry.
go get -x -insecure golang.org/x/time

# Remove container.
docker stop artipie