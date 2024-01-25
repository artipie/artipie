## PyPI Proxy

PyPI proxy repository will proxy all the requests to configured remote and store all the downloaded
packages into configured storage:
```yaml
repo:
  type: pypi-proxy
  storage:
    type: fs
    path: /var/artipie/data
  # optional; if not defined, then will be used settings form `meta` config
  http_client:
    # all fields are optional
    connection_timeout: 25000
    idle_timeout: 500
    trust_all: true
    follow_redirects: true
    http3: true
    jks:
      path: /var/artipie/keystore.jks
      password: secret
    proxies:
      - url: http://proxy1.com
      - url: https://proxy2.com
        # the HTTP "Basic" authentication defined in RFC 2617
        realm: user_realm
        username: user_name
        password: user_password
  remotes:
    - url: https://pypi.org/simple/
      username: alice
      password: 123
```
In the settings `remotes` section should have one `url` item, where
username and password are optional credentials for remote. Provided storage is used as caching feature and 
makes all previously accessed indexes and packages available when remote repository is down.
Check [storage](./Configuration-Storage) documentations to learn more about storage properties.

To install the package with `pip install` specify Artipie repository url with `--index-url` option:

```bash
$ pip install --index-url http://{username}:{password}@{host}:{port}/{repository-name} my-project
```

In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the proxy repository (and repository name is the name of the repo config yaml file),
`username` and `password` are credentials of Artipie user.

