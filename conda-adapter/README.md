<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegram group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/conda-adapter)](http://www.rultor.com/p/artipie/conda-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/conda-adapter.svg)](http://www.javadoc.io/doc/com.artipie/conda-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/conda-adapter/blob/master/LICENSE)
[![codecov](https://codecov.io/gh/artipie/conda-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/conda-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/conda-adapter)](https://hitsofcode.com/view/github/artipie/conda-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/conda-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/conda-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/conda-adapter)](http://www.0pdd.com/p?name=artipie/conda-adapter)

This Java library turns your binary storage (files, S3 objects, anything) into Conda repository.
You may add it to your binary storage and it will become a fully-functionable Conda repository, 
which [anaconda](https://anaconda.org/) will perfectly understand.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/conda-adapter/issues/new)
or contact us in [Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## How to use

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>conda-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Conda-adapter SDK provides methods to add, generate or remove conda packages from `repodata.json` file and
to merge several `repodata.json` files.

To add packages into `repodata.json` file call
```java
new CondaRepodata.Append(Optional.of(input), output).perform(
    Lists.newArrayList(
        new CondaRepodata.PackageItem(
            package_is, "7zip-19.00-h59b6b97_2.conda", "7zip-sha256", "7zip-md5", 123L
        )
    )
);
```
where `CondaRepodata.Append` constructor accepts optional of existing `repodata.json` file input stream 
(pass empty optional if `repodata.json` file does not exist) and output stream to write the result into.
`CondaRepodata.Append#perform` method requires list of the `CondaRepodata.PackageItem`, 
`CondaRepodata.PackageItem` can be created by passing conda package input stream, its name, checksums and 
size into constructor.

To remove packages from `repodata.json` file call
```java
new CondaRepodata.Remove(input, output).perform(
    Set.of("sha256-1", "sha256-2")
);
```
where `CondaRepodata.Remove` constructor accepts existing `repodata.json` file input stream and 
output stream to write the result into. `CondaRepodata.Remove#perform` method requires set of the
SHA-256 checksums of the conda packages to remove from `repodata.json` file.

To merge several `repodata.json` files call
```java
new MultiRepodata.Unique().merge(
    Lists.newArrayList(input_one, input_two), output
);
```
where `MultiRepodata.Unique()#merge` method accepts list of `repodata.json` files input streams to 
merge and output stream to write the result into. While merging, all conda packages duplicates will
be removed.

## Conda repository structure

Conda repository is [structured directory tree](https://docs.conda.io/projects/conda-build/en/latest/resources/package-spec.html#repository-structure-and-index) 
with platform subdirectories, each platform subdirectory contains index file and conda packages. 

```commandline
<root>/linux-64/repodata.json
                repodata.json.bz2
                misc-1.0-np17py27_0.tar.bz2
      /win-32/repodata.json
              repodata.json.bz2
              misc-1.0-np17py27_0.tar.bz2
```

### Repodata file

Repodata json [contains](https://docs.conda.io/projects/conda-build/en/latest/concepts/generating-index.html#repodata-json) 
list of the packages metadata in platform directory and subdir where package is located:

```json
{
  "packages": {
    "super-fun-package-0.1.0-py37_0.tar.bz2": {
      "build": "py37_0",
      "build_number": 0,
      "depends": [
        "some-depends"
      ],
      "license": "BSD",
      "md5": "a75683f8d9f5b58c19a8ec5d0b7f796e",
      "name": "super-fun-package",
      "sha256": "1fe3c3f4250e51886838e8e0287e39029d601b9f493ea05c37a2630a9fe5810f",
      "size": 3832,
      "subdir": "win-64",
      "timestamp": 1530731681870,
      "version": "0.1.0"
    }
  },
  "packages.conda": {
    "super-fun-package-0.2.0-py37_0.conda": {
      "build": "py37_0",
      "build_number": 0,
      "depends": [
        "some-depends"
      ],
      "license": "BSD",
      "md5": "a75683f8d9f5b58c19a8ec5d0b7f797e",
      "name": "super-fun-package",
      "sha256": "1fe3c3f4250e51886838e8e0287e39029d601b9f493ea05c37a2630a9fe5811f",
      "size": 3832,
      "subdir": "win-64",
      "timestamp": 1530731681860,
      "version": "0.2.0"
    }
  }
}
```
`tar.bz2` packages are listed under `packages` element, `.conda` packages are listed under 
`packages.conda` element. `Repodata.json` can also contain some other info 
(for example, subdir name) on the root level.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+ and please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md). 
