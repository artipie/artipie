<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/http-client)](http://www.rultor.com/p/artipie/http)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/http-client.svg)](http://www.javadoc.io/doc/com.artipie/http-client)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/http-client/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/http-client)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/artipie)](https://hitsofcode.com/view/github/artipie/artipie)
![Docker Pulls](https://img.shields.io/docker/pulls/artipie/artipie)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/artipie/artipie?label=DockerHub&sort=date)
[![PDD status](http://www.0pdd.com/svg?name=artipie/artipie)](http://www.0pdd.com/p?name=artipie/artipie)

Artipie is an experimental binary artifact management tool, similar to
[Artifactory](https://jfrog.com/artifactory/),
[Nexus](https://www.sonatype.com/product-nexus-repository),
[Archiva](https://archiva.apache.org/),
[ProGet](https://inedo.com/proget),
and many others.
The following set of features makes Artipie unique among all others:

  * It is open source ([MIT license](https://github.com/artipie/artipie/blob/master/LICENSE.txt))
  * It is horizontally scalable, you can add servers easily
  * It is written in reactive Java (using [Vert.x](https://vertx.io/))
  * It supports
    [Maven](https://github.com/artipie/artipie/wiki/repositories/maven),
    [Docker](https://github.com/artipie/artipie/wiki/repositories/docker),
    [Rubygems](./examples/gem),
    [Go](https://github.com/artipie/artipie/wiki/repositories/go),
    [Helm](https://github.com/artipie/artipie/wiki/repositories/helm),
    [Npm](https://github.com/artipie/artipie/wiki/repositories/npm),
    [NuGet](https://github.com/artipie/artipie/wiki/repositories/nuget),
    [Composer](https://github.com/artipie/artipie/wiki/repositories/composer),
    [Pip](https://github.com/artipie/artipie/wiki/repositories/pypi),
    [Rpm](https://github.com/artipie/artipie/wiki/repositories/rpm),
    [Debian](https://github.com/artipie/artipie/wiki/repositories/debian),
    [Anaconda](https://github.com/artipie/artipie/wiki/repositories/anaconda)
    and [others](./examples)
  * It is database-free
  * It can host the data in the file system or [Amazon S3](https://aws.amazon.com/s3/)
  * Its quality of Java code is extraordinary high :)

# Quickstart

Make sure you have already installed both [Docker Engine](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/).
Then, obtain [`docker-compose.yaml`](https://github.com/artipie/artipie/blob/master/docker-compose.yaml) file
from the repository: you can [open it from the browser](https://github.com/artipie/artipie/blob/master/docker-compose.yaml), 
copy content and save it locally or use [git](https://git-scm.com/) and [clone](https://git-scm.com/docs/git-clone) the repository. 
As soon as Docker Compose is installed and `docker-compose.yaml` file is retrieved, open command line, 
`cd` to the location with the compose file and run Artipie service:

```bash
docker-compose up
```

It'll start a new Docker container with latest Artipie and Artipie dashboard service image. 
Containers should share same config directory, default local mount location is `/usr/local/artipie`,
you may need to correct it if docker client is running not on linux operating system.
A new image generate default configuration if not found at `/etc/artipie/artipie.yml`, prints initial
credentials to console and prints a link to the dashboard. If started on localhost with command
above, the dashboard URI is `http://localhost:8080/dashboard` and default username and password 
are `artipie/artipie`. Artipie server side (repositories) is served on `8081` port and is 
available on URI `http://localhost:8081/artipie/{reponame}`, where `{reponame}` is the name of the
repository.


To create a new artifact repository:
 1. Go to the dashboard
 2. Enter the name of a new repository, choose a type, and click button "Add"
 3. Artipie generates standard configuration for selected kind of repository, and
  asks for review or edit. You can ignore this step for now.
 4. Below the repository configuration, the page will have a simple configuration
  for your client, and usage examples, e.g. the code for `pom.xml` for Maven repository.

Default server configuration refers to `/var/artipie/repos` to look up for repository configurations.
You may want to mount local configurations `<your-local-config-dir>` to `/var/artipie/repos` to edit 
it manually by changing `volumes` values inside `docker-compose.yaml` script.

**Important:** check that `<your-local-config-dir>` has correct permissions, it should be `2020:2021`, 
to change it correctly use `chown -R 2020:2021 <your-local-config-dir>`.

More configuration details and examples are available in our [Wiki](https://github.com/artipie/artipie/wiki).

We recommend you read the "Architecture" section in our [White Paper](https://github.com/artipie/white-paper) 
to fully understand how Artipie is designed.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/artipie/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+ and please read 
[contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md).

Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.

