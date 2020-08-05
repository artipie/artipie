set -x
set -e

# Since we can't run yum on non-rpm based machines
# we will use a docker with yum package manager.
docker network create --driver bridge artipie-rpm-demo

# Remove the network for inter-container communication.
docker network rm artipie-rpm-demo