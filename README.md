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

  * It is open source (MIT license)
  * It is horizontally scalable, you can add servers easily
  * It is written in reactive Java (using [Vert.x](https://vertx.io/))
  * It supports 10+ package managers, incl.
    [Maven](https://maven.apache.org/),
    [NuGet](https://www.nuget.org/),
    [Pip](https://pypi.org/project/pip/),
    [Bundler](https://bundler.io/),
    [Go](https://golang.org/),
    [Docker](https://www.docker.com/), etc.
  * It is database-free
  * It can host the data in the file system, Amazon S3, Google Cloud, etc.
  * Its quality of Java code is extraordinary high :)

The fastest way to start using Artipie is via
[Docker](https://docs.docker.com/get-docker/). First,
create a new directory and a `repo` sub-directory inside it. Then, put your
YAML config file into the `repo` sub-dir. Make sure that the name of your config file
is the name of repository you are going to host, and its name matches `[a-z0-9_]{3,32}`.
For example `foo.yaml`:

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
$ docker run -v "$(pwd):/var/artipie" -p 8080:80 artipie/artipie
```

You should be able to use it with Maven at `http://localhost:8080`.

We recommend you read the "Architecture" section in our
[White Paper](https://github.com/artipie/white-paper) to fully
understand how Artipie is designed.

## Binary Repo

Try this 'repo.yml' file:

```yaml
repo:
  type: rpm
  storage:
    type: fs
    path: /var/artipie/storage
```

You can send HTTP PUT/GET requests
to `http://localhost:8080/repo/<filename>` to upload/download a binary file,
e.g. `http://localhost:8080/repo/libsqlite3.so`.

## Maven Repo

Try this `maven.yaml` file:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /var/artipie/maven
```

Add `<distributionManagement>` to your `pom.xml`:

```xml
<distributionManagement>
  <snapshotRepository>
    <id>artipie</id>
    <url>http://localhost:8080/maven</url>
  </snapshotRepository>
  <repository>
    <id>artipie</id>
    <url>http://localhost:8080/maven</url>
  </repository>
</distributionManagement>
```

Then, `mvn deploy` your project.

Add `<repository>` and `<pluginRepository>`
to your `pom.xml` (alternatively [configure](https://maven.apache.org/guides/mini/guide-multiple-repositories.html)
it via `settings.xml`) to use deployed artifacts:

```xml
<pluginRepositories>
  <pluginRepository>
    <id>artipie</id>
    <name>artipie plugins</name>
    <url>http://localhost:8080/maven</url>
  </pluginRepository>
</pluginRepositories>
<repositories>
  <repository>
    <id>artipie</id>
    <name>artipie builds</name>
    <url>http://localhost:8080/maven</url>
  </repository>
</repositories>
```

Run `mvn install` (or `mvn install -U` to force download dependencies).

## RPM Repo

Create new directory `/var/artipie`, directory for configuration files
`/var/artipie/repo` and directory for RPM repository `/var/artipie/centos`.
Put repository config file to `/var/artipie/repo/centos.yaml`:

```yaml
repo:
  type: rpm
  storage:
    type: fs
    path: /var/artipie/centos
```

Put all RPM packages to repository directory: `/var/artipie/centos/centos`.

Optional: generate metadata using [CLI tool](https://github.com/artipie/rpm-adapter/).

Start Artipie Docker image:

```bash
$ docker run -v /var/artipie:/var/artipie artipie/artipie
```

On the client machine add local repository to the list of repos:

 - Install `yum-utils` if needed: `yum install yum-utils`
 - Add repository: `yum-config-manager --add-repo=http://yourepo/`
 - Refresh the repo: `yum upgrade all`
 - Download packages: `yum install package-name`

## NPM Repo

Try this `npm.yaml` file:

```yaml
repo:
  type: npm
  storage:
    type: fs
    path: /tmp/artipie/data/npm
  permissions:
    admin:
      - \*
    john:
      - deploy
      - delete
    jane:
      - deploy
    \*:
      - download
```

To publish your npm project use the following command:

```bash
$ npm publish --registry=http://localhost:8080/npm
```

## NPM Proxy Repo

Try this `npm-proxy.yaml` file:

```yaml
repo:
  type: npm-proxy
  path: npm-proxy
  storage:
    type: fs
    path: /tmp/artipie/data/npm-proxy
  settings:
    remote:
      url: https://registry.npmjs.org
```

To use it for downloading packages use the following command:

```bash
$ npm install --registry=http://localhost:8080/npm-proxy <package name>
```

or set it as a default repository:

```bash
$ npm set registry http://localhost:8080/npm-proxy
```

## Go Repo

Try this `go.yaml` file:

```yaml
repo:
  type: go
  storage:
    type: fs
    path: /tmp/artipie/data/go
  permissions:
    admin:
      - \*
    \*:
      - download
```

To use it for installing packages add it to `GOPROXY` environment variable:

```bash
$ export GOPROXY="http://localhost:8080/go,https://proxy.golang.org,direct"
```

Go packages have to be located in the local repository by their
names and versions, contain Go module and dependencies information
in `.mod` and `.info` files. Here is an example for package
`example.com/foo/bar` versions `0.0.1` and `0.0.2`:

```
/example.com
  /foo
    /bar
      /@v
        list
        v0.0.1.zip
        v0.0.1.mod
        v0.0.1.info
        v0.0.2.zip
        v0.0.2.mod
        v0.0.2.info
```

`list` is simple text file with list of the available versions.
You can use [go-adapter](https://github.com/artipie/go-adapter#how-it-works)
to generate necessary files and layout for Go source code.

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

Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.
