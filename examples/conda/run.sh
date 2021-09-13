#!/bin/bash

set -e
set -x

# Login (required by anaconda client)
anaconda login --username any --password any

# Upload package.
anaconda upload ./snappy-1.1.3-0.tar.bz2

# Install the package.
conda install -y snappy
