<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/artipie)](http://www.rultor.com/p/artipie/artipie)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/artipie/master.svg)](https://travis-ci.org/artipie/artipie)
![Docker Pulls](https://img.shields.io/docker/pulls/artipie/artipie)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/artipie)](https://hitsofcode.com/view/github/artipie/artipie)
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

## Contents

- [Storage configuration](#storage-configuration)
- [Repository permissions](#repository-permissions)
- [Multitenancy](#multitenancy)
- [Metrics](#metrics)
- [Artipie REST API](#artipie-rest-api)

## Storage configuration

For now, we support two storage types: file system and [S3](https://aws.amazon.com/s3/?nc1=h_ls) storages. 
To configure file system storage it is enough to set the path where Artipie will store all the items:

```yaml
storage:
  type: fs
  path: /urs/local/aripie/data
```

S3 storage configuration requires specifying `bucket` and `credentials`:
```yaml
storage:
  type: s3
  bucket: my-bucket
  region: my-region # optional
  endpoint: https://my-s3-provider.com # optional
  credentials:
    type: basic
    accessKeyId: xxx
    secretAccessKey: xxx
```

Storages can be configured for each repository individually in repo configuration yaml or in 
the `_storages.yaml` file along with aliases:

```yaml
storages:
  default:
    type: fs
    path: ./.storage/data 
```

Then `default` storage alias can be used to configure a repository:

```yaml
repo:
  type: maven
  storage: default
```

## Repository permissions

Permissions for repository operations can be granted in the repo configuration file:
```yaml
repo:
  ...
  permissions:
    jane:
      - read
      - write
    admin:
      - "*"
    /readers:
      - read
```

All repositories support `read` and `write` operations, other specific permissions may be supported 
in certain repository types.

Group names should start with `/`, is the example above `read` operation is granted for `readers` group 
and every user within the group can read from the repository, user named `jane` is allowed to `read` and `write`.
We also support asterisk wildcard for "any operation" or "any user", user `admin` in the example 
can perform any operation in the repository.

If `permissions` section is absent in repo config, then any supported operation is allowed for everyone,
empty `permissions` section restricts any operations for anyone.

## Multitenancy

You may want to run Artipie for your company, which has a few teams.
Each team may want to have its own repository. To do this, you create
a global configuration file `/etc/artipie/artipie.yml`:

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
(`type` is password format, `plain` and `sha256` types are supported):

```yaml
credentials:
  jane:
    type: plain
    pass: qwerty
    email: jane@example.com # Optional
  john:
    type: sha256
    pass: xxxxxxxxxxxxxxxxxxxxxxx
    groups: # Optional
      - readers
      - dev-leads
```
Users can be assigned to some groups, all repository permissions granted to the group are applied 
to the users participating in this group.

If the `type` is set to `env`, the following environment variables are expected:
`ARTIPIE_USER_NAME` and `ARTIPIE_USER_PASS`. For example, you start
Docker container with the `-e` option:

```bash
docker run -d -v /var/artipie:/var/artipie` -p 80:80 \
  -e ARTIPIE_USER_NAME=artipie -e ARTIPIE_USER_PASS=qwerty \
  artipie/artipie:latest
```

## Single repository on port

Artipie repositories may run on separate ports if configured.
This feature may be especially useful for Docker repository,
as it's API is not well suited to serve multiple repositories on single port.

To run repository on its own port 
`port` parameter should be specified in repository configuration YAML as follows:

```yaml
repo:
  type: <repository type>
  port: 54321
  ...
```

*NOTE: Artipie scans repositories for port configuration only on start, 
so server requires restart in order to apply changes made in runtime.* 

## Metrics

You may enable some basic metrics collecting and periodic publishing to application log
by adding `metrics` to `meta` section of global configuration file `/etc/artipie/artipie.yml`:

```yaml
meta:
  metrics:
    type: log # Metrics type, for now only `log` type is supported
    interval: 5 # Publishing interval in seconds, default value is 5
```

## Artipie REST API

Artipie provides a set of APIs to manage repositories and users.  The current APIs are fully documented [here](./REST_API.md).

## Additional configuration

You may want configure it via environment variables:

  - `SSL_TRUSTALL` - trust all unknown certificates
  
To configure repository config files location, add to the global configuration file `/etc/artipie/artipie.yml`:
```yaml
meta:
  repo_configs: configs
```
Location is the storage key relatively to the main storage, or, in file system storage terms, 
subdirectory where repo configs are located relatively to the storage.

Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.

