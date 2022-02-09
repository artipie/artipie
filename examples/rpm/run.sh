#!/bin/bash

set -x
set -e

curl http://artipie.artipie:8080/my-rpm/time-1.7-45.el7.x86_64.rpm --upload-file "./time-1.7-45.el7.x86_64.rpm"
dnf -y repository-packages example install

