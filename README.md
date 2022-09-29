<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/http-client.svg)](http://www.javadoc.io/doc/com.artipie/http-client)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/http-client/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/http-client)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/artipie)](https://hitsofcode.com/view/github/artipie/artipie)
![Docker Pulls](https://img.shields.io/docker/pulls/artipie/artipie)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/artipie/artipie?label=DockerHub&sort=date)
[![PDD status](http://www.0pdd.com/svg?name=artipie/artipie)](http://www.0pdd.com/p?name=artipie/artipie)

Artipie is a binary artifact management tool, similar to
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
    and [others](https://github.com/artipie/artipie/wiki/Configuration-Repository#supported-repository-types)
  * It is database-free
  * It can host the data in the file system, [Amazon S3](https://aws.amazon.com/s3/) or in a storage defined by user
  * Its quality of Java code is extraordinary high :)

Learn more about Artipie in our [Wiki](https://github.com/artipie/artipie/wiki).

# Quickstart

Artipie is distributed as Docker container and as fat `jar`. The `jar` file can be downloaded on the
GitHub [release page](https://github.com/artipie/artipie/releases) and here is a 
[Wiki page](https://github.com/artipie/artipie/wiki#how-to-start-artipie-service-with-a-maven-proxy-repository) describing how to start it.
The fastest way to start Artipie is by using Docker container. First, make sure you have already installed [Docker Engine](https://docs.docker.com/get-docker/).
Then, open command line and instruct Docker Engine to run Artipie container:

```bash
docker run -it -p 8080:8080 -p 8086:8086 artipie/artipie:latest
```

It'll start a new Docker container with latest Artipie version, the command includes mapping of two 
ports: on port `8080` repositories are served and on port `8086` Artipie Rest API and Swagger 
documentation is provided.
A new image generate default configuration, prints a list of running repositories, test 
credentials and a link to the [Swagger](https://swagger.io/) documentation to console. To check 
existing repositories using Artipie Rest API:
- go to Swagger documentation page `http://localhost:8086/api/index-org.html`, 
choose "Auth token" in "Select a definition" list,
- generate and copy authentication token for user `artipie/artipie`,  
- switch to "Repositories" definition, press "Authorize" button and paste the token 
- then perform `GET /api/v1/repository/list` request. 
Response should be a json list with three default repositories:
```json
[
  "artipie/my-bin",
  "artipie/my-docker",
  "artipie/my-maven"
]
```
Artipie server side (repositories) is served on `8080` port and is available on URI 
`http://localhost:8080/{username}/{reponame}`, where `{username}` is the name 
of the user and `{reponame}` is the name of the repository. Let's put some text data into binary repository:
```commandline
curl -X PUT -d 'Hello world!' http://localhost:8080/artipie/my-bin/test.txt
```
With this request we added file `test.txt` containing text "Hello world!" into repository. Let's check
it's really there:
```commandline
curl -X GET http://localhost:8080/artipie/my-bin/test.txt
```
"Hello world!" should be printed in console.

Do dive in dipper into Artipie configuration, features, explore repositories and storages settings, 
please, address our [Wiki](https://github.com/artipie/artipie/wiki).

Default server configuration in Docker Container refers to `/var/artipie/repos` to look up for 
repository configurations. You may want to mount local configurations `<your-local-config-dir>` 
to `/var/artipie/repos` to check and edit it manually.

> **Important:** check that `<your-local-config-dir>` has correct permissions, it should be `2020:2021`,  
to change it correctly use `chown -R 2020:2021 <your-local-config-dir>`.

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

