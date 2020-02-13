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

This is a simple experimental binary artifacts manager.

## How does it work

Artipie uses external server implementation to start itself,
one of possible servers is https://github.com/artipie/vertx-server/

Server accepts `Slice` interface and serves all requests to encapsulated `Slice`.
Server can be started with a single adapter module (since all adapters implements `Slice` interface)
or with Artipie assembly.

Artipie reads server settings from yaml file and contructs
`Storage` to read repositories configurations.

On each request it reads repository name from request URI path
and find related repo configuration in `Storage`. Repo configuration
knows repo type (e.g. `maven` or `docker`) and storage settings for repo
(different repos may have different storage back ends).
After reading repo config it constructs new `Slice` for config
and proxies current request to this slice.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

Thanks to [FreePik](https://www.freepik.com/free-photos-vectors/party) for the logo.
