## Docker Proxy

Artipie Docker Proxy repository redirects all pull requests to specified remote registries:

```yaml
repo:
  type: docker-proxy
  # optional, storage to cache pulled images and to enable push operation
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
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