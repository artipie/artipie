<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/helm-adapter)](http://www.rultor.com/p/artipie/helm-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/helm-adapter.svg)](http://www.javadoc.io/doc/com.artipie/helm-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/helm-adapter/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/helm-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/helm-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/helm-adapter)](https://hitsofcode.com/view/github/artipie/helm-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/helm-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/helm-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/helm-adapter)](http://www.0pdd.com/p?name=artipie/helm-adapter)

# Helm adapter

An Artipie adapter which allow you to host helm carts.  

## Upload a chart

Since helm doesn't officially support chart uploading, the following way is
recommended to use:

```bash
curl --data-binary "@mychart-0.1.0.tgz" http://example.com
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/helm-adapter)
for more technical details.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/helm-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Useful links

[The Chart Repository Guide](https://helm.sh/docs/topics/chart_repository/) - describes repository 
structure and content of the `index.yml` file.

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