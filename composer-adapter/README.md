<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/composer-adapter)](http://www.rultor.com/p/artipie/composer-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/composer-adapter.svg)](http://www.javadoc.io/doc/com.artipie/composer-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/composer-adapter/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/composer-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/composer-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/composer-adapter)](https://hitsofcode.com/view/github/artipie/composer-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/composer-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/composer-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/composer-adapter)](http://www.0pdd.com/p?name=artipie/composer-adapter)

This Java library turns your binary [ASTO](https://github.com/artipie/asto) 
storage into a PHP Composer repository.

Some valuable references:

  * [Composer Documentation](https://getcomposer.org/doc/)
  * [Packagist Private API](https://packagist.com/docs/api)
  * [Composer GitHub](https://github.com/composer)

## Getting started

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>composer-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Save PHP Composer package JSON file like `composer.json` (particular name does not matter)
to [ASTO](https://github.com/artipie/asto) storage.

```java
import com.artipie.asto.*;
Storage storage = new FileStorage(Path.of("/path/to/storage"));
storage.save(
    new Key.From("composer.json"), 
    Files.readAllBytes(Path.of("/my/files/composer.json"))
);
```

Then, make an instance of `Repository` class with storage as an argument.
Finally, instruct `Repository` to add the package to repository:

```java
import com.artipie.composer.*;
Repository repo = new Repository(storage);
repo.add(new Key.From("composer.json"));
```

After that package metadata could be accessed by it's name:

```java
Packages packages = repo.packages(new Name("vendor/package"));
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/composer-adapter)
for more technical details.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/composer-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Project status

- [x] Adding package to repository [#1](https://github.com/artipie/composer-adapter/issues/1)
- [x] HTTP support for adding package as `composer.json` [#22](https://github.com/artipie/composer-adapter/issues/22)
- [x] HTTP support for getting package metadata [#24](https://github.com/artipie/composer-adapter/issues/24)
- [x] HTTP support for adding package as ZIP archive [#23](https://github.com/artipie/composer-adapter/issues/23)

## Composer repository structure
Composer [has](https://getcomposer.org/doc/05-repositories.md#composer) 
the following repository structure:
```
(repository root) 
| -packages.json
| +-vendor
  | -composer.json
  | -composer.lock
  | +-some_vendor
  | | +-package_name
  | | | -files_for_package_name  
  | -autoload.php
  | +-composer
  | | -files_for_composer
```
`composer.lock` file is generated [automatically](https://getcomposer.org/doc/01-basic-usage.md#installing-without-composer-lock).
It is necessary for installing packages of specified versions. If it does not exist, it will be generated
after calling the command `composer install` automatically according to `composer.json` file.  
By calling the command `composer update` `composer.lock` will be automatically updated according 
to existing `composer.json`.

## Content of `composer.json`
There are required packages in this file with specified version. For example, [this file](https://getcomposer.org/doc/01-basic-usage.md#the-require-key).
Also, in this file the type of repository could be defined. There are several types of [repositories](https://getcomposer.org/doc/05-repositories.md#repositories),
but it is necessary to pay attention to [composer](https://getcomposer.org/doc/05-repositories.md#composer) and [artifact](https://getcomposer.org/doc/05-repositories.md#artifact)
repositores.
Example of `composer.json` file for `composer` repository:
```json
{
  "repositories": [ 
    {
      "type": "composer",
      "url": "http://central.artipie.com/"
    },
    {
      "packagist.org": false
    }
  ],
  "require": { 
    "psr/log": "1.1.3" 
  }
}
```
Example of `composer.json` file for `artifact` repository:
```json
{
  "repositories": [ 
    {
      "type": "artifact",
      "url": "path/to/directory/with/zips/"
    },
    {
      "packagist.org": false
    }
  ],
  "require": { 
    "psr/log": "1.1.3" 
  }
}
```

## Packages index file
The only [required](https://getcomposer.org/doc/05-repositories.md#packages) field is `package`. An example of JSON file:
```json
{
  "packages": {
    "vendor/package-name": {
      "0.0.1": { 
        "name (required)": "vendor/package-name",
        "version (required)": "0.0.1",
        "dist (required)": {
          "url": "https://host:port/path/vendor/package-name-0.0.1.zip",
          "type": "zip"
        } 
      },
      "1.0.0": { "similar structure": "" }
    }
  }
}
```
So, information about a specific version of package is obtained from remote source by specifying url.

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

