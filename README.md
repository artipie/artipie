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
  * It supports
    [Maven](./examples/maven),
    [Docker](./examples/docker),
    [Rubygems](./examples/gem),
    [Go](./examples/go),
    [Helm](./examples/helm),
    [Npm](./examples/npm),
    [NuGet](./examples/nuget),
    [Composer](./examples/php),
    [Pip](./examples/pypi),
    [Rpm](./examples/rpm),
    and [others](./examples)
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
$ docker run -p 8080:80 artipie/artipie:latest
```

You should be able to use it with [Maven](https://maven.apache.org/)
at `http://localhost:8080`.

More examples are [here](./examples).

We recommend you read the "Architecture" section in our
[White Paper](https://github.com/artipie/white-paper) to fully
understand how Artipie is designed.

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
    type: log # Metrics type, for now only `log` type is supported
    interval: 5 # Publishing interval in seconds, default value is 5
```

## Additional configuration

You may want configure it via environment variables:

  - `SSL_TRUSTALL` - trust all unkown certificates

Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.

