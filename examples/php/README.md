### PHP Composer Repo

Try this `my-php.yaml` file:

```yaml
repo:
  type: php
  path: my-php
  storage:
    type: fs
    path: /tmp/artipie/data/my-php
```

To publish your PHP Composer package create package description JSON file `my-package.json`
with the following content:

```json
{
  "name": "my-org/my-package",
  "version": "1.0.0",
  "dist": {
    "url": "https://www.my-org.com/files/my-package.1.0.0.zip",
    "type": "zip"
  }
}
```

And add it to repository using PUT request:

```bash
$ curl -X PUT -T 'my-package.json' "http://localhost:8080/my-php"
```

To use this library in your project add requirement and repository to `composer.json`:

```json
{
    "repositories": [
         {"type": "composer", "url": "http://localhost:8080/my-php"}
    ],
    "require": {
        "my-org/my-package": "1.0.0"
    }
}
```