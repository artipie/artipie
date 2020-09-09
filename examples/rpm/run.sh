set -x
set -e

# Since we can't run yum on non-rpm based machines
# we will use a docker with yum package manager.
docker network create --driver bridge artipie-rpm-demo

# Start Artipie.
docker run --network artipie-rpm-demo --rm --name artipie -d -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Post a wget package.
curl -i  -X PUT  --data-binary "@wget-1.19.5-8.el8_1.1.x86_64.rpm" http://localhost:8080/my-rpm/wget-1.19.5-8.el8_1.1.x86_64.rpm

# Start centos container and install wget from artipie.
docker run --network artipie-rpm-demo --rm --name centos centos:8 \
 /bin/bash -e -c "\
  yum install -y yum-utils ; \
  yum-config-manager --add-repo http://artipie:80/my-rpm/; \
  yum --skip-broken --disablerepo='*' --enablerepo='80_my-rpm_' install wget"

# Stop Artipie.
docker stop artipie

# Remove the network for inter-container communication.
docker network rm artipie-rpm-demo