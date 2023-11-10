<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/gem-adapter)](http://www.rultor.com/p/artipie/gem-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/gem-adapter.svg)](http://www.javadoc.io/doc/com.artipie/gem-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/gem-adapter/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/gem-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/gem-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/gem-adapter)](https://hitsofcode.com/view/github/artipie/gem-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/gem-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/gem-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/gem-adapter)](http://www.0pdd.com/p?name=artipie/gem-adapter)


`gem-adapter` is a SDK for managing Gem repositories with low-level operations and HTTP endpoint for Gem repository.

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>gem-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

`gem-adapter` is a slice in Artipie, aimed to support gem packages.
Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/gem-adapter)
for more technical details.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/gem-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Usage

There are two public APIs for working with Gem repository:
 - low-level `Gem` SDK
 - high-level `GemSlice` HTTP endpoint

### Gem SDK

Create a new instance of `Gem` class with: `new Gem(storage)`,
where `storage` is a [asto](https://github.com/artipie/asto) `Storage` implementation
with Gem repository.

To **update** repository with a new `gem` package use `gem.update(key)`, where `key` is a package key in storage.

For retreiving package spec info use `gem.info(key)` method with, where `key` is a package key in storage.
It returns future with `MetaInfo` interface, which can be printed to one of standard formats with `meta.print(fmt)`:
 - `JsonMetaFormat` - for JSON meta spec format
 - `YamlMetaFormat` - for YAML meta spec format

To extract **dependencies** binary metadata of packages, use `gem.dependencies(names)` method, where
`names` is a set of gem names (`Set<String>`); this method returns future with binary dependencies metadata
merged for multiple packages.

### HTTP endpoint

To integrate Gem HTTP endpoint to server, use `GemSlice` class instance: `new GemSlice(storage)`, where
`storage` is a repository storage for gem packages. This `Slice` implementation exposes standard Gem repository
APIs and could be used by `gem` CLI.

## Useful links

* [RubyGem Index Internals](https://blog.packagecloud.io/eng/2015/12/15/rubygem-index-internals/) - File structure and gem format
* [Make Your Own Gem](https://guides.rubygems.org/make-your-own-gem/) - How to create and publish
a simple ruby gem into rubygems.org registry.
* [rubygems.org API](https://guides.rubygems.org/rubygems-org-api/) - A page with rubygems.org 
API specification 
* [Gugelines at rubygems.org](https://guides.rubygems.org/) - Guidelines around the `gem` package 
manager.
 
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

