set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Push a gem into artipie.
cd sample-project
gem build sample-project.gemspec
GEM_HOST_API_KEY=1 gem push sample-project-1.0.0.gem --host http://localhost:8080/my-gem
cd ..

# Fetch the uploaded earlier gem from artipie.
GEM_HOST_API_KEY=1 gem fetch sample-project --source http://localhost:8080/my-gem

# Remove container.
docker stop artipie