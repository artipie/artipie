<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegram group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/maven-adapter)](http://www.rultor.com/p/artipie/maven-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/maven-adapter.svg)](http://www.javadoc.io/doc/com.artipie/maven-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/maven-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/maven-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/maven-adapter)](https://hitsofcode.com/view/github/artipie/maven-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/maven-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/maven-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/maven-adapter)](http://www.0pdd.com/p?name=artipie/maven-adapter)

This Java library is turns binary storage (files, S3 objects, anything) into Maven repository. It 
implements and can work with Maven repository structure and provides fully-functionable 
[`mvn`](https://maven.apache.org/) support in [Artipie](https://github.com/artipie/artipie) 
binary repository manager service.

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/maven-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## Maven repository concepts

### Files

A dependency is identified by its _coordinates_ - groupId, artifactId and version.
JAR files may contain a classifier suffix - `sources` or `javadoc`.
By coordinates you can determine the artifact path in local or remote repository and vice versa.

Repositories must handle following types of files:
- A primary artifact - a main JAR file.
- Secondary artifacts - a POM file, files with a classifier.
- Attribute files - checksums, signatures, lastUpdate files for primary and secondary artifacts.
- Metadata files - `maven-metadata.xml`, containing information about artifact versions
including snapshot versions.

File naming convention is:
`artifactId-version[-classifier]-extension`

### Layout

(Default) naming convention is - in groupId replace all dots with directory separator ('/')
then put artifactId, version and then go files.

Example layout (not fully exhaustive):
```
$ROOT
|-- org/
    `-- example/
        `-- artifact/
            `-- maven-metadata.xml
            `-- maven-metadata.xml.sha1
            `-- 1.0/
                |-- artifact-1.0.jar
                |-- artifact-1.0.jar.sha1
                |-- artifact-1.0.pom
                |-- artifact-1.0.pom.sha1
                |-- artifact-1.0-sources.jar
                |-- artifact-1.0-sources.jar.sha1
            `-- 2.0-SNAPSHOT/
                |-- artifact-2.0-20210409.123503.jar
                |-- artifact-2.0-20210409.123503.jar.sha1
                |-- artifact-2.0-20210409.123503.pom
                |-- artifact-2.0-20210409.123503.pom.sha1
                |-- artifact-2.0-20210412.163503.jar
                |-- artifact-2.0-20210412.163503.jar.sha1
                |-- artifact-2.0-20210412.163503.pom
                |-- artifact-2.0-20210412.163503.pom.sha1
                |-- maven-metadata.xml
                |-- maven-metadata.xml.sha1
```

For example, for an artifact `org.example:artifact:1.0` (Gradle-style notation is used for clarity)
the path would be `org/example/artifact/1.0/artifact-1.0.jar` (and other files).

### Snapshot support
Maven supports the use of `snapshot` repositories. These repositories are used only when resolving `SNAPSHOT` dependencies.
`SNAPSHOT` dependencies are just like regular dependencies, with `-SNAPSHOT` appended to it:

```xml
<dependency>
    <groupId>com.artipie</groupId>
    <artifactId>maven-adapter</artifactId>
    <version>2.0-SNAPSHOT</version>
</dependency>
```

This feature allows anyone which depends on the `SNAPSHOT` version get the latest changes on every build. 

In the repository layout snapshots subdirectories usually contain several versions of the package, 
files creation timestamps are appended to the filenames. Also, snapshots have their own maven metadata.

### Upload process
On deploy maven client sends to the server package artifacts with the help of the `PUT` HTTP requests, 
at the end of the deploy process maven client sends package metadata. 
Here an example of maven request set:
```commandline
PUT /com/artipie/helloworld/1.0/helloworld-1.0.jar
PUT /com/artipie/helloworld/1.0/helloworld-1.0.jar.sha1
PUT /com/artipie/helloworld/1.0/helloworld-1.0.pom
PUT /com/artipie/helloworld/1.0/helloworld-1.0.pom.sha1
GET /com/artipie/helloworld/maven-metadata.xml
PUT /com/artipie/helloworld/maven-metadata.xml
PUT /com/artipie/helloworld/maven-metadata.xml.sha1
```

Uploaded data are saved to the temporary upload location with the following layout:
```commandline
|-- .upload
  `-- com
     `--example
        `-- logger
          `-- 0.1-SNAPSHOT
              |-- logger-0.1.jar
              |-- logger-0.1.jar.sha1
              |-- logger-0.1.jar.md5
              |-- logger-0.1.pom
              |-- logger-0.1.pom.sha1
              |-- logger-0.1.pom.md5
              |-- maven-metadata.xml             # snapshot metadata
              |-- maven-metadata.xml.sha1
              |-- maven-metadata.xml.md5
                `-- meta
                    |-- maven-metadata.xml       # package metadata
                    |-- maven-metadata.xml.sha1
                    |-- maven-metadata.xml.md5
```

Repository update is started when an artifact (any, nondeterministic) and package maven-metadata.xml 
have the same set of checksums. On the repository update checksums are verified, 
package metadata are processed and all the received artifacts are saved to the repository. 

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```
To avoid build errors use Maven 3.2+ and please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md).