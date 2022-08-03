## PyPI Proxy

PyPI proxy repository will proxy all the requests to configured remote and store all the downloaded
packages into configured storage:
```yaml
repo:
  type: pypi-proxy
  remotes:
    - url: https://pypi.org/simple/
      username: alice
      password: 123
      cache:
        storage:
          type: fs
          path: /var/artipie/data
  permissions:
    mark:
      - "*"
```
In the settings `remotes` section should have one `url` item with cache storage (the cache storage is required),
username and password are optional credentials for remote. Caching feature makes all previously accessed 
indexes and packages available when remote repository is down.
Check [storage](../Configuration-Storage.md) and [permission](../Configuration-Repository%20Permissions.md)
documentations to learn more about its properties.

To install the package with `pip install` specify Artipie repository url with `--index-url` option:

```bash
$ pip install --index-url http://{username}:{password}@{host}:{port}/{repository-name} my-project
```

In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the proxy repository (and repository name is the name of the repo config yaml file),
`username` and `password` are credentials of Artipie user.

