#!/bin/bash

set -x
set -e

yum-config-manager --add-repo http://artipie.artipie:8080/my-rpm/
yum --skip-broken --disablerepo='*' --enablerepo='artipie.artipie_8080_my-rpm_' install wget

