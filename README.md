<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/yegor256/artipie)](http://www.rultor.com/p/yegor256/artipie)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/yegor256/artipie/master.svg)](https://travis-ci.org/yegor256/artipie)
[![Javadoc](http://www.javadoc.io/badge/com.yegor256/artipie.svg)](http://www.javadoc.io/doc/com.yegor256/artipie)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/yegor256/artipie/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/yegor256/artipie)](https://hitsofcode.com/view/github/yegor256/artipie)
[![Maven Central](https://img.shields.io/maven-central/v/com.yegor256/artipie.svg)](https://maven-badges.herokuapp.com/maven-central/com.yegor256/artipie)
[![PDD status](http://www.0pdd.com/svg?name=yegor256/artipie)](http://www.0pdd.com/p?name=yegor256/artipie)

Artipie is an experimental binary artifacts manager, similar to
[Artifactory](https://jfrog.com/artifactory/),
[Nexus](https://www.sonatype.com/product-nexus-repository),
[Archiva](https://archiva.apache.org/),
[ProGet](https://inedo.com/proget),
and many others.
The following set of features makes Artipie unique among all others:

  * It is open source (MIT license)
  * It is horizontally scalable, you can add servers easily
  * It is written in reactive Java (using [Vert.x](https://vertx.io/))
  * It supports 10+ package managers, incl. Maven, NuGet, Pip, Gem, etc.
  * It is database-free
  * It can host the data in the file system, Amazon S3, Google Cloud, etc.
  * Its quality of Java code is extraordinary high :)

The fastest way to start using Artipie is via
[Docker](https://docs.docker.com/get-docker/). First,
create a new directory and a `repo` sub-directory inside it. Then, put your
YAML config file into the `repo` sub-dir. Make sure that the name of your config file
is the name of repository you are going to host, for example `foo.yaml`:

```yaml
repo:
  type:
    maven
  storage:
    type: fs
    path: /var/artipie
    username: admin
    password: 123qwe
```

Finally, start the container:

```bash
$ docker run -v "$(pwd):/var/artipie" -p 80:8080 artipie/artipie
```

You should be able to use it with, for example, Maven, as `http://localhost:8080`.

More usage examples are [here](https://github.com/artipie/artipie/wiki/Examples).

We recommend you read the "Architecture" section in our
[White Paper](https://github.com/artipie/white-paper) to fully
understand how Artipie is designed.

## YAML Config File

Repository configuration is a yaml file located at configuration
directory root. Repository name will be the same as configuration file name (with `yaml` extension).
A valid repository name must be 3-33 characters, may contain lower
Latin characters, digits and underscores: `[a-z0-9_]{3,32}`:

 - `my_rpm_repo1` - valid
 - `2maven` - valid
 - `my-repo` - invalid
 - `MyRepo` - invalid

Repository configuration consists in two parts (some parts are optional):

  * `type` (required) - repository type name, one of
    `maven`, `file`, `rpm`, `nuget`, `npm`, `docker`, `go`, `php`, `npm-proxy`
  * `storage` (required) - repository storage configuration, may be different for some storages
  * `permissions` (optional) - permission settings, repository considered to be public if `permissions` is ommited

Example:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /var/artipie/maven
  permissions:
    user_name:
      - download
      - deploy
```

The `storage/type` field can be `fs` for file system or `s3` for S3 blob storage API.

File system storage should contain the location of storage root in the local file system:

```yaml
storage:
  type: fs
  path: /var/artipie
```

S3 storage configuration contains these parameters:

 - `bucket` (required) - S3 bucket name
 - `region` (optional) - AWS region name
 - `url` (optional) - S3 URL
 - `endpoint` (optional) - S3 endpoint, can be used for non-AWS S3 API
 - `credentials` (required) - S3 credentials
   - `type` (required) - credentials type (can be `basic` for now)
   - `accessKeyId` - AWS API access key for user with S3 bucket read and write permissions
   - `secretAccessKey` - AWS API secret key for access key

Example:

```yaml
storage:
  type: s3
  region: eu-central-1
  url: s3://artipie.test/binary
  endpoint: https://s3.eu-central-1.amazonaws.com
  bucket: artipie.test
  credentials:
    type: basic
    accessKeyId: <access-key>
    secretAccessKey: <secret-key>
```

## How to contribute

Fork the repository, make changes, and send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.
