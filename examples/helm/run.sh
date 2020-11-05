set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Upload a helm chage
curl -i -X POST --data-binary "@tomcat-0.4.1.tgz" http://localhost:8080/example_helm_repo/

# Add a repository and make sure it works 
helm repo add artipie_example_repo http://localhost:8080/example_helm_repo/
helm repo update

# Remove container.
docker stop artipie
