#!/bin/bash

set -x
set -e

dnf --skip-broken -y repository-packages example install time

