<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/nuget-adapter)](http://www.rultor.com/p/artipie/nuget-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/nuget-adapter.svg)](http://www.javadoc.io/doc/com.artipie/nuget-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/com.artipie/artipie/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/artipie/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/nuget-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/nuget-adapter)](https://hitsofcode.com/view/github/artipie/nuget-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/nuget-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/nuget-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/nuget-adapter)](http://www.0pdd.com/p?name=artipie/nuget-adapter)

This Java library turns your binary [ASTO](https://github.com/artipie/asto) 
storage (binary, Amazon S3 objects) into a NuGet repository. It provides NuGet repository 
support for [Artipie](https://github.com/artipie) distribution and allows you to use `nuget` client
commands (such as `nuget push` and `nuget install`) to work with NuGet packages. Besides, NuGet-adapter
can be used as a library to parse `.nuget` packages files and obtain package metadata.

Some valuable references:

  * [NuGet Documentation](https://docs.microsoft.com/en-us/nuget/)
  * [NuGet Sources](https://github.com/NuGet)

## NuGet-adapter SDK API

Add dependency to `pom.xml`:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>nuget-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Save NuGet package ZIP file like `package.nupkg` (particular name does not matter)
to [ASTO](https://github.com/artipie/asto) storage. 
Then, make an instance of `Repository` class with storage as an argument.
Finally, instruct `Repository` to add the package to repository:

```java
import com.artipie.nuget;
Repository repo = new Repository(storage);
repo.add(new Key.From("package.nupkg"));
```

You may also use lower level classes to parse `.nupkg` files and read package `.nuspec` file:
```java
// create instance of NuGetPackage
final NuGetPackage pkg = new Nupkg(Files.newInputStream(Paths.get("my_example.nupkg")));
// read `.nuspec` metadata
final Nuspec nuspec = pkg.nuspec();

final NuspecField id = nuspec.id(); // get packages id
final NuspecField veersion = nuspec.version(); // get package version
```
Instance of `NuspecField` classes allows to obtain both raw and normalized 
(according to Nuget normalization rules) values of the fields. `Nuspec` allows to get description,
authors, packages types and any other `.nuspec` metadata fields value. 

Class `Version` can be used to normalise the version, it also implements `Comparable<Version>` 
interface and can be used to sort the package by versions.

To add the package into `index.json` [registration page](https://learn.microsoft.com/en-us/nuget/api/registration-base-url-resource#registration-page-object)
use `IndexJson.Update` class:

```java
// you can add the package to existing index.json
final IndexJson.Update index = new IndexJson.Update(Files.newInputStream(Paths.get("my_index.json")));

// or if index.json does not exists, use empty constructor
final IndexJson.Update index = new IndexJson.Update();

// then call `perform()` method providing the package to add
final JsonObject res = upd.perform(new Nupkg(Files.newInputStream(Paths.get("my_example.nupkg"))));
// resulting JsonObject represents index.json with added package
```

To remove package from `index.json` [registration page](https://learn.microsoft.com/en-us/nuget/api/registration-base-url-resource#registration-page-object)
use `IndexJson.Delete` class:

```java
// create `IndexJson.Delete` instance providing index.json to remove the package from
final IndexJson.Delete index = new IndexJson.Delete(Files.newInputStream(Paths.get("my_index.json")));

//then call `perform()` method package id and version which will be deleted
final JsonObject res = upd.perform("package-to-delete-id", "package-to-delete-version");
// resulting JsonObject contains index.json with removed package
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/nuget-adapter)
for more technical details.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/artipie/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

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

The test suite of this project include some integration tests which require NuGet client to be installed.
NuGet client may be downloaded from official site [nuget.org](https://www.nuget.org/downloads).
Integration tests could also be skipped using Maven's `skipITs` options:

```
$ mvn clean install  -DskipITs
```
