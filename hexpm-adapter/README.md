<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/hexpm-adapter)](http://www.rultor.com/p/artipie/hexpm-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/hexpm-adapter.svg)](http://www.javadoc.io/doc/com.artipie/hexpm-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/hexpm-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/hexpm-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/hexpm-adapter)](https://hitsofcode.com/view/github/artipie/hexpm-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/hexpm-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/npm-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/hexpm-adapter)](http://www.0pdd.com/p?name=artipie/hexpm-adapter)

## Hexpm adapter for Elixir and Erlang packages.

This Java library, piece of Artipie, which allows you to create your own private package repository for Erlang and Elixir.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/hexpm-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

> For generate classes, first use the command:
> ```shell
> mvn compile
> ```

## Structure

Repository contains two type of files: **package** - meta-information about package and **tarball** - archive with package.  
When publishing a tar archive, the repository creates(or updates if exist) a package file. Package file is gzipped file that include bytes in protobuf format.

**Package** consist of:  
```
-- bytes(gzipped bytes):
    -- signed(SignedOuterClass.Signed):
        -- sign(String)
        -- package(PackageOuterClass.Package):
            -- name(String)
            -- repository(str), default:"artipie"
            -- releases(List<PackageOuterClass.Release>):
                -- release(PackageOuterClass.Release)
                    -- version(String)
                    -- innerchecksum(ByteString)
                    -- outerchecksum(ByteString)
                    -- outerchecksum(ByteString)
                    -- dependencies(List<PackageOuterClass.Dependency>):
                        -- dependency(PackageOuterClass.Dependency)
                            -- package(String)
                            -- requirement(String)
                            -- repository(str), default:"artipie"
                            -- optional(Boolean)
```
More information about structure you can find in [proto files](src/main/resources/proto).

**Tarball** contains:
```
-- contents.tar.gz - gzipped tarball with project contents
-- CHECKSUM - SHA-256 hex-encoded checksum of the included tarball
-- metadata.config - Erlang term file with project's metadata
-- VERSION - tarball version, current version is 3.
```
More information in [tarball specification](https://github.com/hexpm/specifications/blob/main/package_tarball.md).

## How to configure

An example with a simple configuration can be found on the [Artipie wiki page](https://github.com/artipie/artipie/wiki/hexpm).

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+ and please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md).
