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
  type:
    maven
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

### Binary Repo

Try this `repo.yaml` file:

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

### Maven Repo

Try this `maven.yaml` file to host a [Maven](https://maven.apache.org/) repo:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /var/artipie/maven
```

Add [`<distributionManagement>`](https://maven.apache.org/pom.html#Distribution_Management)
section to your
[`pom.xml`](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
(don't forget to specify authentication credentials in
[`~/.m2/settings.xml`](https://maven.apache.org/settings.html)):

```xml
<project>
  [...]
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
</project>
```

Then, `mvn deploy` your project.

Add [`<repository>`](https://maven.apache.org/pom.html#Repositories) and
[`<pluginRepository>`](https://maven.apache.org/pom.html#Repositories)
to your `pom.xml` (alternatively
[configure](https://maven.apache.org/guides/mini/guide-multiple-repositories.html)
it via
[`~/.m2/settings.xml`](https://maven.apache.org/settings.html)) to use deployed artifacts:

```xml
<project>
  [...]
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
</project>
```

Run `mvn install` (or `mvn install -U` to force download dependencies).

### Maven proxy Repo

Try this `maven-central.yaml` file to host a proxy to Maven central:

```yaml
repo:
  type: maven-proxy
  storage: default
```

Artipie will redirect all Maven requests to Maven central.
Add it [as a mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html)
to `settings.xml`:
```xml
<settings>
  <mirrors>
    <mirror>
      <id>artipie-mirror</id>
      <name>Artipie Mirror Repository</name>
      <url>https://central.artipie.com/mirrors/maven-central</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### RPM Repo

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

### NPM Repo

Try this `npm.yaml` file:

```yaml
repo:
  type: npm
  path: /npm
  storage:
    type: fs
    path: /tmp/artipie/data/npm
  permissions:
    john:
      - download
      - upload
    jane:
      - upload
```

To publish your npm project use the following command:

```bash
$ npm publish --registry=http://localhost:8080/npm
```

### NPM Proxy Repo

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

### Go Repo

Try this `go.yaml` file:

```yaml
repo:
  type: go
  path: go
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

### PHP Composer Repo

Try this `my-php.yaml` file:

```yaml
repo:
  type: php
  path: my-php
  storage:
    type: fs
    path: /tmp/artipie/data/my-php
```

To publish your PHP Composer package create package description JSON file `my-package.json`
with the following content:

```json
{
  "name": "my-org/my-package",
  "version": "1.0.0",
  "dist": {
    "url": "https://www.my-org.com/files/my-package.1.0.0.zip",
    "type": "zip"
  }
}
```

And add it to repository using PUT request:

```bash
$ curl -X PUT -T 'my-package.json' "http://localhost:8080/my-php"
```

To use this library in your project add requirement and repository to `composer.json`:

```json
{
    "repositories": [
         {"type": "composer", "url": "http://localhost:8080/my-php"}
    ],
    "require": {
        "my-org/my-package": "1.0.0"
    }
}
```

### NuGet Repo

Try this `nuget.yaml` file:

```yaml
repo:
  type: nuget
  path: my-nuget
  url: http://localhost:8080/my-nuget
  storage:
    type: fs
    path: /tmp/artipie/data/my-nuget
```

To publish your NuGet package use the following command:

```bash
$ nuget push my.lib.1.0.0.nupkg -Source=http://localhost:8080/my-nuget/index.json
```

To install the package into a project use the following command:

```bash
$ nuget install MyLib -Version 1.0.0 -Source=http://localhost:8080/my-nuget/index.json
```

### Gem Repo

Try this `gem.yaml` file:

```yaml
repo:
  type: gem
  storage:
    type: fs
    path: /tmp/artipie/data/my-nuget
```

Publish a gem:

```bash
$ gem push my_first_gem-0.0.0.gem --host http://localhost:8080/gem
```

Install a gem:

```bash
$ gem install my_first_gem --source http://localhost:8080/gem
```

### Helm chart repo

Try this `helm.yaml` file:

```yaml
repo:
  type: helm
  storage:
    type: fs
    path: /tmp/artipie/data/helm-charts
```

Publish a chart:

```bash
$ curl --data-binary "@my_chart-1.6.4.tgz" http://localhost:8080/helm
```

Install a chart:

```bash
$ helm repo add artipie http://localhost:8080/helm/charts
$ helm install my_chart artipie
```

### Docker Repo

Try this `docker.yaml` file:

```yaml
repo:
  type: docker
  path: my-docker
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

## Multi-Team Setup

You may want to run Artipie for your company, which has a few teams. Each
team may want to have its own repository. To do this, you create
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
