<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegram group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/pypi-adapter)](http://www.rultor.com/p/artipie/pypi-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/pypi-adapter.svg)](http://www.javadoc.io/doc/com.artipie/pypi-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/pypi-adapter/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/pypi-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/pypi-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/pypi-adapter)](https://hitsofcode.com/view/github/artipie/pypi-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/pypi-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/pypi-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/pypi-adapter)](http://www.0pdd.com/p?name=artipie/pypi-adapter)

This Java library turns Python packages storage (files, S3 objects, anything) 
into Python Repository which `pip` and `twine` can perfectly understand. This library 
is a part of [Artipie](https://github.com/artipie) binary artifact management tool and provides 
Python storage functionality for [Artipie central](https://central.artipie.com/). 

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/pypi-adapter/issues/new) 
or contact us in [Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## How to set up `pip` and `twine` to use Artipie central PyPI repository 

To **install** packages with `pip` add the following section to `.pip/pip.conf`:

```shell script
[global]
extra-index-url = https://myname:mypass@central.artipie.com/my_pypi
```

To **upload** using `pip` create valid `~/.pypirc` file:

```shell script
[distutils]
index-servers = example

[example]
repository = https://central.artipie.com/my_pypi
username = myname
password = mypass
```

To **upload** package with `twine` specify `--repository-url`:

```shell script
python3 -m twine upload --repository-url https://central.artipie.com/my_pypi -u myname -p mypass --verbose my_project/dist/*
```

To use a private server with setuptools unittesting add the following to your `setup.py`:

```
from setuptools import setup

setup(
    ...
    dependency_links=[
        'https://myname:mypass@central.artipie.com/my_pypi/my_project/'
    ])
```

## PYPI repository API and file structure

PYPI repository API is explained by [PEP-503](https://www.python.org/dev/peps/pep-0503/).
The repository root `/` API must return a valid HTML5 page with a single anchor element per project in the repository:
```html
<!DOCTYPE html>
<html>
  <body>
    <a href="/frob/">frob</a>
    <a href="/spamspamspam/">spamspamspam</a>
  </body>
</html>
```

These links may be helpful:
 - Simple repository layout https://packaging.python.org/guides/hosting-your-own-index/
 - Repository API https://www.python.org/dev/peps/pep-0503/

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn verify -Pqulice
```

To avoid build errors use Maven 3.2+ and please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md). 