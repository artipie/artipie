#!/bin/bash
set -x
set -e

# Build and upload python project to artipie.
cd sample-project
python3 -m pip install --user --upgrade setuptools wheel
python3 setup.py sdist bdist_wheel
python3 -m pip install --user --upgrade twine
python3 -m twine upload --repository-url http://artipie.artipie:8080/my-pypi \
  -u username -p password dist/*
cd ..

# Install earlier uploaded python package from artipie.
python3 -m pip install --index-url http://artipie.artipie:8080/my-pypi sample_project
