<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegram group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/asto-core.svg)](http://www.javadoc.io/doc/com.artipie/asto-core)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/asto/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/asto/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/asto)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/asto)](https://hitsofcode.com/view/github/artipie/asto)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.artipie/asto-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/asto-core)
[![PDD status](http://www.0pdd.com/svg?name=artipie/asto)](http://www.0pdd.com/p?name=artipie/asto)

Asto stands for Abstract Storage, an abstraction over physical data storage system.
The main entity of the library is an interface `com.artipie.asto.Storage`, a contract
which requires to implement the following functionalities:

* put/get/delete operations
* transaction support
* list files in a directory
* check if a file/directory exists
* provide file metadata (size, checksums, type, etc.)

Dictionary used for ASTO:
 - `Storage` - key-value based storage
 - `Key` - storage keys, could be converted to strings
 - `Content` - storage data, reactive publisher with optional size attribute
 - `SubStorage` - isolated storage based on origin storage


The list of back-ends supported:
 - FileStorage - file-system based storage, uses paths as keys, stores content in files
 - S3Storage - uses S3 compatible HTTP web-server as storage, uses keys as names and blobs for content
 - EtcdStorage - uses ETCD cluster as storage back-end
 - InMemoryStorage - storage uses `HashMap` to store data
 - RedisStorage - storage based on [Redisson](https://github.com/redisson/redisson)


This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>asto-core</artifactId>
  <version>[...]</version>
</dependency>
```

The following dependency allows using RedisStorage:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>asto-redis</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/asto-core) for more technical details. 
If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/asto/issues/new) 
or contact us in [Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

# Usage

The main entities here are:
 - `Storage` interface provides API for key-value storage
 - `Key` represents storage key
 - `Content` represents storage binary value

[Storage](https://www.javadoc.io/doc/com.artipie/asto/latest/com/artipie/asto/Storage.html),
[Key](https://www.javadoc.io/doc/com.artipie/asto/latest/com/artipie/asto/Key.html) and other entities are
documented in [javadoc](https://www.javadoc.io/doc/com.artipie/asto/latest/index.html).

Here is en example of how to create `FileStorage`, save and then read some data:
```java
final Storage asto = new FileStorage(Path.of("/usr/local/example"));
final Key key = new Key.From("hello.txt");
asto.save(
    key,
    new Content.From("Hello world!".getBytes(StandardCharsets.UTF_8))
).thenCompose(
    ignored -> asto.value(key)
).thenCompose(
    val -> new PublisherAs(val).asciiString()
).thenAccept(
    System.out::println
).join();
```
In the example we created local text file `/usr/local/example/hello.txt` containing string "Hello world!",
then read and print it into console. Used classes:
- `Key.From` is implementation of the `Key` interface, keys are strings, separated by `/`
- `Content.From` implements `Content` interface, allows to create `Content` instances from 
  byte arrays or [publisher](https://www.reactive-streams.org/reactive-streams-1.0.4-javadoc/org/reactivestreams/Publisher.html) of ByteBuffer's
- `PublisherAs` class allows to fully read `Content` into memory as byte arrays

Note, that `Storage` is asynchronous and always returns `CompletableFutures` as a result, use 
future chains (`thenAccept()`, `thenCompose()` etc.) and call blocking methods `get()` or `join()` 
when necessary.

Other storage implementations (`S3Storage`, `InMemoryStorage`, `RedisStorage`) can be used in the same way, only
constructors differ, here is an example of how to create `S3Storage` instance:

```java
final Storage asto = new S3Storage(
    S3AsyncClient.builder().credentialsProvider(
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create("accessKeyId", "secretAccessKey")
        )
    ).build(),
    "bucketName"
);
```

To get more details about `S3AsyncClient` builder, check 
[Java S3 client docs](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3AsyncClient.html).

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

