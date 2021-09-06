#!/bin/bash

set -e
set -x

# Set conda automatic upload
conda config --set anaconda_upload yes

# Login (required by anaconda client)
anaconda login --username any --password any

# Build and push package.
conda build --output-folder ./conda-out/ ./example-package/conda/

# Install the package.
conda install -y examplepackage
