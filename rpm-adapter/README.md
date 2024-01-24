<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/rpm-adapter)](http://www.rultor.com/p/artipie/rpm-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/rpm-adapter.svg)](http://www.javadoc.io/doc/com.artipie/rpm-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/artipie/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/artipie/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/rpm-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/rpm-adapter)](https://hitsofcode.com/view/github/artipie/rpm-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/rpm-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/rpm-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/rpm-adapter)](http://www.0pdd.com/p?name=artipie/rpm-adapter)

This Java library turns your binary storage
(files, S3 objects, anything) into an RPM repository.
You may add it to your binary storage and it will become
a fully-functionable RPM repository, which
[`yum`](https://en.wikipedia.org/wiki/Yum_%28software%29) and
[`dnf`](https://en.wikipedia.org/wiki/DNF_%28software%29)
will perfectly understand.

Similar solutions:

  * [Artifactory](https://www.jfrog.com/confluence/display/RTF/RPM+Repositories)
  * [Pulp](https://pulp-rpm.readthedocs.io/en/latest/)

Some valuable references:

  * [RPM format](https://rpm-packaging-guide.github.io/)
  * [Yum repository internals](https://blog.packagecloud.io/eng/2015/07/20/yum-repository-internals/) (blog post)
  * [YUM repository and package management: Complete Tutorial](https://www.slashroot.in/yum-repository-and-package-management-complete-tutorial) (blog post)
  * [The Origin of RPM Content](https://docs.pulpproject.org/en/2.9/plugins/pulp_rpm/tech-reference/rpm.html)

If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/artipie/issues/new) 
or contact us in [Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## How to use

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>rpm-adapter</artifactId>
  <version>[...]</version>
</dependency>
```
 
Use one of the existing `com.artipie.asto.Storage` implementations to create the `Storage`. 
[`Artipie/asto`](https://github.com/artipie/asto) supports
[`FileStorage`](https://github.com/artipie/asto/blob/master/src/main/java/com/artipie/asto/fs/FileStorage.java), 
[`S3`](https://github.com/artipie/asto/blob/master/src/main/java/com/artipie/asto/s3/S3Storage.java) 
and other storages. Or you can implement `com.artipie.asto.Storage` by yourself.

Then, you make an instance of `Rpm` class with your storage
as an argument. Finally, you put your artifacts to the storage specifying repository key 
(`rpm-repo` in our example) and instruct `Rpm` to update the meta info:

```java
import com.artipie.rpm.Rpm;
final Storage storage = new FileStorage(Paths.get("my-artipie"));
final String name = "rpm-repo";
storage.save(
    new Key.From(name, "pkg.rpm"), 
    new Content.From(Files.readAllBytes(Paths.get("pkg.rpm")))
).join();
final Rpm rpm = new Rpm(storage);
rpm.batchUpdate(new Key.From(name));
```

Read the [Javadoc](https://www.javadoc.io/doc/com.artipie/artipie/latest/index.html)
for more technical details.

### Naming policy and checksum computation algorithm

RPM may use different names to store metadata files in the package,
by default we use `StandardNamingPolicy.PLAIN`. To change naming policy use
secondary constructor of `Rpm` to configure it. For instance to add `SHA1` prefixes for metadata 
files use `StandardNamingPolicy.SHA1`. 

RPM may use different algorithms to calculate rpm packages checksum for metadata. By default, we use 
`sha-256` algorithms for hashing. To change checksum calculation algorithm use secondary 
constructor of `Rpm` and `Digest` enum to specify the algorithm:

```java
Rpm rpm = new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256);
```

### Include filelists.xml metadata

RPM repository may include `filelists.xml` metadata, this metadata is not required by all rpm package
managers. By default, we do not generate this metadata file but this behaviour can be configured 
with the help of `Rpm` secondary constructor.

## How it works?

First, you upload your `.rpm` artifact to the repository. Then,
you call `batchUpdate()` and these four system XML files are updated
in the `repodata` directory:
`repomd.xml`, `primary.xml.gz`, `filelists.xml.gz`, and `others.xml.gz`.

Examples of these files you can find in
[this repo](https://download.docker.com/linux/centos/7/source/stable/repodata/).

## Cli

Build the Cli tool using `mvn clean package -Pcli`.
You can run it as following
```bash
java -jar target/rpm-adapter.jar ./repo-dir/
```

Options are:
- `naming-policy` - (optional, default `simple`) configures NamingPolicy for Rpm
- `digest` - (optional, default `sha256`) configures Digest instance for Rpm
- `filelists` - (optional, default `true`) includes File Lists for Rpm
- `update` - (optional, default empty, no update) allows to set schedule to update repository in 
cron format. This option allows performing repository update periodically, according to schedule.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install 
```

To avoid build errors use Maven 3.2+ and please read [contributing rules](https://github.com/artipie/artipie/blob/master/CONTRIBUTING.md). 
