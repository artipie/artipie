set -x
set -e

# Start artipie.
docker run --rm -d --name artipie -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Create a file for subsequent publication.
echo "hello world" > text.txt

# Publish text.txt.
curl --silent -X PUT --data-binary "@text.txt" http://localhost:8080/repo/text.txt

# Download the file.
STATUSCODE=$(curl --silent --output /dev/stderr --write-out "%{http_code}" -X GET http://localhost:8080/repo/text.txt)

# Remove container.
docker stop artipie

# Make sure status code is 200.
if test $STATUSCODE -ne 200; then
  exit 1
fi
