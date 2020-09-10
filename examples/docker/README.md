### Docker Repo

![](https://github.com/artipie/artipie/workflows/Proof::docker/badge.svg)

This directory contains a basic example of how can Artipie be used as a Docker registry.
Try this example by running `run.sh` script.

Basic configuration `my-docker.yaml`:

```yaml
repo:
  type: docker
  storage:
    type: fs
    path: /var/artipie/data
```

After creating the configuration file below, Artipie is ready to server as a Docker registry.

Before pushing any images let's pull an existing one from Docker Hub:

```bash
docker pull ubuntu
```

Since the Docker registry is going to be located at localhost:8080/my-docker, we tag the pulled
image respectively:

```bash
docker image tag ubuntu localhost:8080/my-docker/myfirstimage
```

Afterwards we have to login, since Artipie Docker registry support only authorized users:

```bash
docker login --username alice --password qwerty123 localhost:8080
```

And finally, we a ready to push the pulled image:

```bash
docker push localhost:8080/my-docker/myfirstimage
```

The image can be pulled as well:

```bash
# Pull the pushed image from artipie.
docker image rm localhost:8080/my-docker/myfirstimage
docker pull localhost:8080/my-docker/myfirstimage
```

#### Advanced options

##### Security

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

##### Docker Proxy Repo

Try this `docker-proxy.yaml` file to host a proxy to `registry-1.docker.io` registry:

```yaml
repo:
  type: docker-proxy
  remotes:
    - url: registry-1.docker.io
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
  remotes:
    - url: mcr.microsoft.com
      cache:
        storage:
          type: fs
          path: /tmp/artipie/data/my-docker-cache
```

It is possible to push images using `docker push` command to proxy storage
and store them locally if `storage` is specified as follows:

```yaml
repo:
  type: docker-proxy
  remotes:
    - url: registry-1.docker.io
    - url: mcr.microsoft.com
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
```
