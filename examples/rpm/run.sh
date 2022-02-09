#!/bin/bash

set -x
set -e

dnf config-manager --add-repo http://artipie.artipie:8080/my-rpm/
dnf --skip-broken --disablerepo='*' --enablerepo='artipie.artipie_8080_my-rpm_' install wget

