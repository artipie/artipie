<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/files-adapter)](http://www.rultor.com/p/artipie/files-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/files-adapter.svg)](http://www.javadoc.io/doc/com.artipie/files-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/files-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/files-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/files-adapter)](https://hitsofcode.com/view/github/artipie/files-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/files-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/files-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/vertx-server)](http://www.0pdd.com/p?name=artipie/files-adapter)

This is a simple storage, used in a few other projects.

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>files-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/files-adapter)
for more technical details.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/files-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Running

For simple cases or for debugging you may want to start files-adapter as HTTP server.
Just build it and start with:
```bash
mvn package dependency:copy-dependencies
java -cp "target/files-adapter-1.0-SNAPSHOT.jar:target/dependency/*" com.artipie.files.FilesSlice
```
This command builds service and start it with in-memory storage on localhost on `8080` port.

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

