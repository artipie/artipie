#!/bin/bash -ex
docker push artipie/conan-tests:1.0
docker push artipie/conda-tests:1.0
docker push artipie/deb-tests:1.0
docker push artipie/docker-tests:1.0
docker push artipie/file-tests:1.0
docker push artipie/helm-tests:1.0
docker push artipie/maven-tests:1.0
docker push artipie/pypi-tests:1.0
docker push artipie/rpm-tests-fedora:1.0
docker push artipie/rpm-tests-ubi:1.0
