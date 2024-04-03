# Quickstart with docker-compose

Make sure you have already installed both [Docker Engine](https://docs.docker.com/get-docker/) and
[Docker Compose](https://docs.docker.com/compose/install/).
Then, obtain `docker-compose.yaml` file from the repository:
you can [open it from the browser](https://github.com/artipie/artipie/blob/master/artipie-main/docker-compose.yaml),
copy content and save it locally or use [git](https://git-scm.com/) and [clone](https://git-scm.com/docs/git-clone) the repository.
Open command line, `cd` to the location with the compose file and run Artipie service:

```bash
docker-compose up
```

It'll start new Docker containers with latest Artipie and Artipie dashboard service images.
New Artipie's container generates default configuration if not found at `/etc/artipie/artipie.yml`, 
prints to console a list of running repositories, test credentials and a link to the [Swagger UI](https://swagger.io/tools/swagger-ui/).

If started on localhost with command above: 
* The dashboard URI is `http://localhost:8080/dashboard` and default username and password are `artipie/artipie`.
* Swagger UI to manage Artipie is available on 'http://localhost:8086/api/index.html'. More information about [Rest API](./Rest-api) 
* Artipie server side (repositories) is served on `8081` port and is available on URI `http://localhost:8081/{reponame}`, 
where `{reponame}` is the name of the repository.