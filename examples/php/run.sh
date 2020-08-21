set -x
set -e

# Start artipie.
docker run --rm --name artipie -d -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
sleep 5

# Make a zip package and post it to artipie binary storage.
zip -r sample-for-deployment.zip sample-for-deployment
curl -i -X PUT --data-binary "@sample-for-deployment.zip" http://localhost:8080/bin/sample-for-deployment.zip

# Post the package to php-composer-repository.
curl -i -X POST  http://localhost:8080/my-php \
--request PUT \
-H "Expect:" \
-H 'Content-Type: application/json; charset=utf-8' \
--data-binary @- << EOF
{
  "name": "artipie/sample_composer_package",
  "version": "1.0",
  "dist": {
    "url": "http://localhost:8080/bin/sample-for-deployment.zip",
    "type": "zip"
  }
}
EOF

# Install the deployed package.
cd sample-consumer; rm -rf vendor/ composer.lock
composer install

# Remove container.
docker stop artipie
