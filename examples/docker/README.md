### Docker Repo

Try this `docker.yaml` file:

```yaml
repo:
  type: docker
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
```

Docker registry has to be protected by HTTPS and should have no prefix in path.
In order to access this Docker repository it is required to run a reverse proxy such as
[nginx](https://nginx.org/) or [lighttpd](https://www.lighttpd.net/) to protect Artipie
with HTTPS and add forwarding of requests from `my-docker.my-company.com/<path>` to
`my-artipie.my-company.com/my-docker/<path>`.
Then to push your Docker image use the following command:

```bash
$ docker push my-docker.my-company.com/my-image
```

To pull the image use the following command:

```bash
$ docker pull my-docker.my-company.com/my-image
```

### Docker Proxy Repo

Try this `docker-proxy.yaml` file to host a proxy to `mcr.microsoft.com` registry:

```yaml
repo:
  type: docker-proxy
  settings:
    host: mcr.microsoft.com
```

Artipie will redirect all pull requests to specified registry.

Proxy repository supports caching in local storage.
To enable it and make previously accessed images available when source repository is down 
add `storage` section to config:

```yaml
repo:
  type: docker-proxy
  settings:
    host: mcr.microsoft.com
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker-cache
```
