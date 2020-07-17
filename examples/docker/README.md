### Docker Repo

![](https://github.com/artipie/artipie/workflows/Proof::docker/badge.svg)

Try this `docker.yaml` file:

```yaml
repo:
  type: docker
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
```

Docker registry has to be protected by HTTPS.

Tag your image with `central.artipie.com/{{user}}/{{name}}` image prefix,
and push it to central.artipie.com then. E.g.
for `alpine:3.11` use:
<pre>
docker tag alpine:3.11 central.artipie.com/{{user}}/{{name}}/alpine:3.11
docker push central.artipie.com/{{user}}/{{name}}/alpine:3.11
</pre>


### Docker Proxy Repo

Try this `docker-proxy.yaml` file to host a proxy to `registry-1.docker.io` registry:

```yaml
repo:
  type: docker-proxy
  settings:
    host: registry-1.docker.io
    username: Aladdin # optional
    password: OpenSesame # optional
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
