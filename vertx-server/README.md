<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/vertx-server)](http://www.rultor.com/p/artipie/vertx-server)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/vertx-server.svg)](http://www.javadoc.io/doc/com.artipie/vertx-server)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE)
[![codecov](https://codecov.io/gh/artipie/artipie/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/vertx-server)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/vertx-server)](https://hitsofcode.com/view/github/artipie/vertx-server)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/vertx-server.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/vertx-server)
[![PDD status](http://www.0pdd.com/svg?name=artipie/vertx-server)](http://www.0pdd.com/p?name=artipie/vertx-server)

This is a http server based on [HttpServer](https://vertx.io/docs/apidocs/index.html?io/vertx/reactivex/core/http/HttpServer.html) 
from [Vert.x Core](https://vertx.io/docs/vertx-core/java/), used by [artipie](https://github.com/artipie/artipie) 
project and by some artipie`s adapters (for example, [maven-adapter](https://github.com/artipie/maven-adapter),
[rpm-adapter](https://github.com/artipie/rpm-adapter)) to implement integration tests.

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>vertx-server</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/vertx-server)
for more technical details.
If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/artipie/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Usage example

```java
final Vertx vertx = Vertx.vertx();
final int port = 8080;
final Slice slice = ...; // some Slice implementation
final VertxSliceServer server = new VertxSliceServer(vertx, slice, port);
server.start();
```

## How to contribute

Please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md).

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

