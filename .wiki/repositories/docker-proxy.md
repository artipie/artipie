## Docker Proxy

Artipie Docker Proxy repository redirects all pull requests to specified remote registries:

```yaml
repo:
  type: docker-proxy
  remotes:
    - url: registry-1.docker.io
    - url: mcr.microsoft.com
      userename: alice # optional, remote login
      password: abc123 # optional, remote password
      cache: # optional, storage to cache pulled from remote images
        storage:
          type: fs
          path: /tmp/artipie/data/my-docker-cache
  # optional, storage for pushed images
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
```
In the `remotes` section at least one `url` is required. Credentials can be set in `userename`
and `password` fields. Proxy repository also supports caching of pulled images in local storage
if optional `storage` section is configured for the remote. When several remotes are specified, Artipie
will try to request the image from each remote while the image is not found.

When `storage` section under `meta` section in configured, it is also possible to push images
using `docker push` command to proxy repository and store them locally.

Find the example how to pull and push images into docker registry in [Docker repository section](./docker.md#usage-example).