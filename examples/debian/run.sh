set -x
set -e

# Start Artipie.
docker run --name artipie -d -it -v $(pwd)/artipie.yaml:/etc/artipie/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Add Artipie to Debian repository sources by modifying sources.list file.
touch sources.list
echo "deb [trusted=yes] http://localhost:8080/my-debian my-debian main" >> sources.list
sudo mv sources.list /etc/apt

# Post a package.
curl -i  -X PUT  --data-binary "@aglfn_1.7-3_amd64.deb" http://localhost:8080/my-debian/main/aglfn_1.7-3_amd64.deb

# Update the world and install posted package.
sudo apt-get update
sudo apt-get install -y aglfn

# Stop Artipie.
docker stop artipie