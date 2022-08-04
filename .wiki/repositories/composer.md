## Composer

Composer repository is a dependency manager and packages sharing tool for [PHP packages](https://getcomposer.org/).
Here is the configuration example:
```yaml
repo:
  type: php
  url: http://{host}:{port}/{repository-name}
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    "*":
      - download
    john:
      - upload
```
The Composer repository configuration requires `url` field that contains repository full URL,
`{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file). Check
[storage](../Configuration-Storage.md) and [permission](../Configuration-Repository-Permissions.md)
documentations to learn more about these settings.

To upload the file into repository, use `PUT` HTTP request:
```bash
curl -X PUT -T 'log-1.1.4.zip' "http://{host}:{port}/{repository-name}/log-1.1.4.zip"
```
To use packages from Artipie repository in PHP project, add requirement and repository to `composer.json`:
```json
{
  "config": { "secure-http": false },
  "repositories": [
    { "type": "composer", "url": "http://{host}:{port}/{repository-name}" },
    { "packagist.org": false }
  ],
  "require": { "log": "1.1.4" }
}
```