<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/artipie)](http://www.rultor.com/p/artipie/artipie)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/artipie/master.svg)](https://travis-ci.org/artipie/artipie)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/artipie.svg)](http://www.javadoc.io/doc/com.artipie/artipie)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/artipie)](https://hitsofcode.com/view/github/artipie/artipie)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/artipie.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/artipie)
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
  * It supports 10+ package managers, incl.
    [Maven](https://maven.apache.org/),
    [NuGet](https://www.nuget.org/),
    [Pip](https://pypi.org/project/pip/),
    [Gem](https://guides.rubygems.org/what-is-a-gem/),
    [Go](https://golang.org/),
    [Docker](https://www.docker.com/), etc.
  * It is database-free
  * It can host the data in the file system,
    [Amazon S3](https://aws.amazon.com/s3/),
    [Google Cloud](https://cloud.google.com/products/storage/),
    [HuaweiCloud OBS](https://www.huaweicloud.com/en-us/product/obs.html) etc.
  * Its quality of Java code is extraordinary high :)

The fastest way to start using Artipie is via
[Docker](https://docs.docker.com/get-docker/). First,
create a new directory `artipie` and `repo` sub-directory inside it. Then, put your
YAML config file into the `repo` sub-dir. Make sure that the name of your config file
is the name of repository you are going to host, and its name matches `[a-z0-9_]{3,32}`.
For example `foo.yaml`:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /var/artipie
```

Now, go back to `artipie` and start the container:

```bash
$ docker run -v "$(pwd):/var/artipie" -p 8080:80 artipie/artipie
```

You should be able to use it with [Maven](https://maven.apache.org/)
at `http://localhost:8080`.

In the sections below you can see how to configure Artipie
to use it with different package managers.

We recommend you read the "Architecture" section in our
[White Paper](https://github.com/artipie/white-paper) to fully
understand how Artipie is designed.

## Additional configuration

Environment variables:
 - `SSL_TRUSTALL` - trust all unkown certificates

## Multitenancy

You may want to run Artipie for your company, which has a few teams.
Each team may want to have its own repository. To do this, you create
a global configuration file `/etc/artipie.yml`:

```yaml
meta:
  layout: org
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
  credentials:
    type: file
    path: _credentials.yml
```

If the `type` is set to `file`, another YAML file is required in the storage, with
a list of users who will be allowed to create repos
(each `pass` is combination or either `plain` or `sha256` and a text):


```yaml
credentials:
  jane:
    pass: "plain:qwerty"
  john:
    pass: "sha256:xxxxxxxxxxxxxxxxxxxxxxx"
```

If the `type` is set to `env`, the following environment variables are expected:
`ARTIPIE_USER_NAME` and `ARTIPIE_USER_PASS`. For example, you start
Docker container with the `-e` option:

```bash
docker run -d -v /var/artipie:/var/artipie` -p 80:80 \
  -e ARTIPIE_USER_NAME=artipie -e ARTIPIE_USER_PASS=qwerty \
  artipie/artipie:latest
```

## Metrics

You may enable some basic metrics collecting and periodic publishing to application log
by adding `metrics` to `meta` section of global configuration file `/etc/artipie.yml`:

```yaml
meta:
  metrics:
    type: log
```

## How to contribute

Fork the repository, make changes, and send us a
[pull request](https://www.yegor256.com/2014/04/15/github-guidelines.html). We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

## How to run it locally

To run Artipie server locally, build it with `mvn clean package -Passembly`
and run with *(change port if needed)*:
```java
java -jar target/artipie-jar-with-dependencies.jar --config=example/artipie.yaml --port=8080
```
Example configuration uses `org` layout of Artipie with two level hierarchy,
user `test` with password `123`, and `default` storage in `./example/storage` direcotry.
To access the dashboard open `http://localhost/test` in your browser and enter user credentials.


Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.
