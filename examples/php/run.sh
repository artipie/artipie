#!/bin/bash

set -x
set -e

# Make a zip package and post it to artipie binary storage.
zip -r sample-for-deployment.zip sample-for-deployment
curl -i -X PUT --data-binary "@sample-for-deployment.zip" http://artipie.artipie:8080/bin/sample-for-deployment.zip

# Post the package to php-composer-repository.
curl -i -X POST  http://artipie.artipie:8080/my-php \
--request PUT \
--data-binary @- << EOF
{
  "name": "artipie/sample_composer_package",
  "version": "1.0",
  "dist": {
    "url": "http://artipie.artipie:8080/bin/sample-for-deployment.zip",
    "type": "zip"
  }
}
EOF

# Install the deployed package.
cd sample-consumer; rm -rf vendor/ composer.lock
composer install
