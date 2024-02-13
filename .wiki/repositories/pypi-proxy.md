## PyPI Proxy

PyPI proxy repository will proxy all the requests to configured remote and store all the downloaded
packages into configured storage:
```yaml
repo:
  type: pypi-proxy
  storage:
    type: fs
    path: /var/artipie/data
  http_client: # optional, settings for the HttpClient that will be used in xxx-proxy repositories
    connection_timeout: 25000 # optional, default 15000 ms 
    idle_timeout: 500 # optional, default 0
    trust_all: true # optional, default false
    follow_redirects: true # optional, default true
    http3: true # optional, default false
    jks: # optional
      path: /var/artipie/keystore.jks
      password: secret
    proxies:
      - url: http://proxy1.com
      - url: https://proxy2.com
        # the HTTP "Basic" authentication defined in RFC 2617
        realm: user_realm # if this field is defined, then `username` and `password` are mandatory
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

