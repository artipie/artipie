#!/bin/bash

set -x
set -e

cat /etc/yum.repos.d/example.repo
curl -i -X PUT --data-binary "@time-1.7-45.el7.x86_64.rpm" http://artipie.artipie:8080/my-rpm/time-1.7-45.el7.x86_64.rpm
dnf -y repository-packages example install

