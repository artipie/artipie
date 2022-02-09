#!/bin/bash

set -x
set -e

curl -i -X PUT --data-binary "@time-1.7-45.el7.x86_64.rpm" http://artipie.artipie:8080/my-rpm/time-1.7-45.el7.x86_64.rpm
curl -i -X GET http://artipie.artipie:8080/my-rpm/repodata/repomd.xml
dnf -y repository-packages example install

