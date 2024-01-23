<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/docker-adapter)](http://www.rultor.com/p/artipie/docker-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/docker-adapter.svg)](http://www.javadoc.io/doc/com.artipie/docker-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/com.artipie/artipie/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/artipie/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/docker-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/docker-adapter)](https://hitsofcode.com/view/github/artipie/docker-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/docker-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/docker-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/docker-adapter)](http://www.0pdd.com/p?name=artipie/docker-adapter)

Docker registry front and back end as Java dependency: front end includes all HTTP API functions
for Docker clients, back end provides classes to work with default registry file structure.
Back end depends on https://github.com/artipie/asto storage, so it can be served on file system, S3 or
other supported storages.

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/docker-adapter)
for more technical details.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/artipie/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Specification

Front end supports non-blocking requests processing, this is why the back-end uses `Flow` API from JDK9.
ASTO storage system also supports non-blocking data processing (in case of TCP/HTTP APIs, FS operations are
always blocking).

Registry documentation:
 - https://docs.docker.com/registry/introduction/
 - https://docs.docker.com/registry/spec/api/

The path layout in the storage backend is roughly as follows:

```
<root>/v2
    -> repositories/
        -> <name>/
            -> _manifests/
                revisions
                -> <manifest digest path>
                    -> link
                tags/<tag>
                -> current/link
                    -> index
                    -> <algorithm>/<hex digest>/link
                -> _layers/
                      <layer links to blob store>
                -> _uploads/<id>
                      data
                      startedat
                      hashstates/<algorithm>/<offset>
    -> blob/<algorithm>
        <split directory content addressable storage>
```

More detailed explanation of registry storage system see at SPEC.md file.

## How to contribute

Please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md).

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install
```

To avoid build errors use Maven 3.2+.
