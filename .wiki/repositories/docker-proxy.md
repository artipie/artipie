## Docker Proxy

Artipie Docker Proxy repository redirects all pull requests to specified remote registries:

```yaml
repo:
  type: docker-proxy
  # optional, storage to cache pulled images and to enable push operation
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
  # optional; if not defined, then will be used settings from a `meta` config
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
    - url: registry-1.docker.io
    - url: mcr.microsoft.com
      username: alice # optional, remote login
      password: abc123 # optional, remote password
```
In the `remotes` section at least one `url` is required. Credentials can be set in `userename`
and `password` fields. Proxy repository also supports caching of pulled images in local storage
if optional `storage` section is configured. When several remotes are specified, Artipie
will try to request the image from each remote while the image is not found.

When `storage` section under `meta` section in configured, it is also possible to push images
using `docker push` command to proxy repository and store them locally.

Find the example how to pull and push images into docker registry in [Docker repository section](./docker#usage-example).