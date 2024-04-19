#!/bin/bash -e
baseDir="$(dirname $0)"
cd "$baseDir"

rm -rf ./_context

mkdir -p ./_context/projects
cp -rfv ../artipie-main/src/test/resources/helloworld-src  ../artipie-main/src/test/resources/snapshot-src _context/projects
cp -rfv ../maven-adapter/src/test/resources-binary/helloworld-src _context/projects/helloworld-adapter
sed -i 's/host.testcontainers.internal:%d/localhost:8080/' _context/projects/helloworld-adapter/pom.xml.template
mv -v _context/projects/helloworld-adapter/pom.xml.template _context/projects/helloworld-adapter/pom.xml
cp -rfv ../artipie-main/src/test/resources/com/artipie ../artipie-main/src/test/resources/maven _context
cp -fv ./fetchPomDeps.sh _context/
echo '<settings></settings>' > _context/settings.xml
MAVER_VER="1.0"
docker build --progress=plain --build-arg MAVER_VER="$MAVER_VER" -t "artipie/maven-tests:$MAVER_VER" _context -f ./Dockerfile.maven

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/conda ./_context/ # example-project; *.bz2; etc.
cp -rfv ../conda-adapter/src/test/resources-binary/example-project ./_context/adapter/
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
CONDA_VER="1.0"
docker build --progress=plain --build-arg CONDA_VER="$CONDA_VER" -t "artipie/conda-tests:$CONDA_VER" _context -f ./Dockerfile.conda

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/rpm ./_context/
cp -rfv ../rpm-adapter/src/test/resources-binary/ ./_context/adapter
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
RPM_FVER="1.0"
docker build --progress=plain --build-arg RPM_FVER="$RPM_FVER" -t "artipie/rpm-tests-fedora:$RPM_FVER" _context -f ./Dockerfile.rpm_fedora

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/rpm ./_context/
cp -rfv ../rpm-adapter/src/test/resources-binary/ ./_context/adapter
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
RPM_UVER="1.0"
docker build --progress=plain --build-arg RPM_UVER="$RPM_UVER" -t "artipie/rpm-tests-ubi:$RPM_UVER" _context -f ./Dockerfile.rpm_fedora

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/debian ./_context/
cp -rfv ../debian-adapter/src/test/resources/ ./_context/adapter
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
DEB_VER="1.0"
docker build --progress=plain --build-arg DEB_VER="$DEB_VER" -t "artipie/deb-tests:$DEB_VER" _context -f ./Dockerfile.debian

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/conan ./_context/
cp -rfv ../conan-adapter/src/test/resources/ ./_context/adapter
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
CONAN_VER="1.0"
docker build --progress=plain --build-arg CONAN_VER="$CONAN_VER" -t "artipie/conan-tests:$CONAN_VER" _context -f ./Dockerfile.conan

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/pypi-repo ./_context/
cp -rfv ../pypi-adapter/src/test/resources/ ./_context/adapter
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
PYPI_VER="1.0"
docker build --progress=plain --build-arg PYPI_VER="$PYPI_VER" -t "artipie/pypi-tests:$PYPI_VER" _context -f ./Dockerfile.pypi

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/docker ./_context/
cp -rfv ../docker-adapter/src/test/resources/ ./_context/adapter
cp -rfv ./prepMinio.sh ../artipie-main/src/test/resources/minio-bin-20231120.txz ./_context/
DOCKER_VER="1.0"
docker build --progress=plain --build-arg DOCKER_VER="$DOCKER_VER" -t "artipie/docker-tests:$DOCKER_VER" _context -f ./Dockerfile.docker
# Caching test images. Needs privileged mode.
TMP_CONT="local-artipie-docker-test-cache"
docker stop "$TMP_CONT" || :
docker container rm "$TMP_CONT" || :
docker run --name "$TMP_CONT" --privileged artipie/docker-tests:1.0 sh -c 'rc-service docker start;sleep 0.5;docker pull alpine:3.11;docker pull alpine:3.19.1;docker pull debian:stable-slim;rc-service docker stop;sleep 0.5;'
hash=$(docker commit "$TMP_CONT")
docker tag "$hash" "artipie/docker-tests:$DOCKER_VER"
docker stop "$TMP_CONT"
docker container rm "$TMP_CONT"

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/file-repo ./_context/
cp -rfv ../pypi-adapter/src/test/resources/ ./_context/adapter
FILE_VER="1.0"
docker build --progress=plain --build-arg FILE_VER="$FILE_VER" -t "artipie/file-tests:$FILE_VER" _context -f ./Dockerfile.file

rm -rf ./_context

mkdir -p ./_context/adapter
cp -rfv ../artipie-main/src/test/resources/helm ./_context/
cp -rfv ../helm-adapter/src/test/resources/ ./_context/adapter
HELM_VER="1.0"
docker build --progress=plain --build-arg FILE_VER="$HELM_VER" -t "artipie/helm-tests:$HELM_VER" _context -f ./Dockerfile.helm

rm -rf ./_context
