#!/bin/bash -e
if [ $# -ne 1 ]; then
    echo "Usage: $0 path" && exit 1
fi

pom="$1"
groupId=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='groupId']/text()" "$pom")
artifactId=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='artifactId']/text()" "$pom")
version=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" "$pom")
artifact="${groupId}:${artifactId}:${version}"
mvn -B -e -f "$pom" dependency:go-offline
mvn -B -e -f "$pom" dependency:go-offline deploy -Dmaven.deploy.skip=true -Dartifact="$artifact"
mvn -B -e -f "$pom" versions:set -DnewVersion=1.0-SNAPSHOT
