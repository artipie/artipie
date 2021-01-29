#!/bin/bash

# Upload a helm chage
curl -i -X POST --data-binary "@tomcat-0.4.1.tgz" \
  http://artipie.artipie:8080/example_helm_repo/

# Add a repository and make sure it works 
helm repo add artipie_example_repo http://artipie.artipie:8080/example_helm_repo/
helm repo update
