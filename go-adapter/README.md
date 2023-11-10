<a href="http://artipie.com"><img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/></a>

[![Join our Telegramm group](https://img.shields.io/badge/Join%20us-Telegram-blue?&logo=telegram&?link=http://right&link=http://t.me/artipie)](http://t.me/artipie)

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/go-adapter)](http://www.rultor.com/p/artipie/go-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.artipie/go-adapter.svg)](http://www.javadoc.io/doc/com.artipie/go-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/goproxy/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/go-adapter/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/go-adapter)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/go-adapter)](https://hitsofcode.com/view/github/artipie/go-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/go-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/go-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/go-adapter)](http://www.0pdd.com/p?name=artipie/go-adapter)

This Java library turns your storage
(files, S3 objects, anything) with Go sources into
a Go repository.

Some valuable references:

  * [Module proxy protocol](https://golang.org/cmd/go/#hdr-Module_proxy_protocol)
  * [Why you should use a Go module proxy](https://arslan.io/2019/08/02/why-you-should-use-a-go-module-proxy/)
  * [Go Modules Are Awesome, But There Is One Tiny Problem](https://jfrog.com/blog/go-modules-are-awesome-but-there-is-one-tiny-problem/)

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>go-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Then, you implement `com.artipie.asto.Storage` interface
and pass it to the instance of `Goproxy`. Then, you
let it know when is the right moment to update certain artifact:

```java
Goproxy goproxy = new Goproxy(storage);
goproxy.update("example.com/foo/bar", "0.0.1");
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/goproxy)
for more technical details.
If you have any question or suggestions, do not hesitate to [create an issue](https://github.com/artipie/go-adapter/issues/new) or contact us in
[Telegram](https://t.me/artipie).  
Artipie [roadmap](https://github.com/orgs/artipie/projects/3).

## How it works?

It is assumed that package sources are located in the repository
by their package names. For example, if you have two packages, they
may be located like this:

```
/foo
  /first
    go.mod
    foo.go
    LICENSE.txt
/bar
  /second
    go.mod
    bar.go
```

Then, when you are ready to release a new version `0.0.1`, you call
`update("example.com/foo/first", "0.0.1").blockingAwait()` or `update("example.com/foo/first", "0.0.1").subscribe()` for async execution.
Four new files will be created:

```
/example.com
  /foo
    /first
      /@v
        list
        v0.0.1.zip
        v0.0.1.mod
        v0.0.1.info
```

These files are needed for Go to understand that the package is ready
to be used.
There are samples of these files from Google repository:
[`.mod`](https://proxy.golang.org/github.com/liujianping/ts/@v/v0.0.7.mod),
[`.info`](https://proxy.golang.org/github.com/liujianping/ts/@v/v0.0.7.info),
[`.zip`](https://proxy.golang.org/github.com/liujianping/ts/@v/v0.0.7.zip),
and
[`list`](https://proxy.golang.org/github.com/liujianping/ts/@v/list).

When you decide to release another version, three additional files will
be created when you call `update("example.com/foo/first", "0.0.2").blockingAwait()`:

```
/example.com
  /foo
    /first
      /@v
        list
        v0.0.1.zip
        v0.0.1.mod
        v0.0.1.info
        v0.0.2.zip
        v0.0.2.mod
        v0.0.2.info
```

The file `list` will be updated.

## Go module proxy protocol

The most common way to get any source code, modules or packages while working with go is to use 
go tool and `go get` command. This command can fetch modules directly from vcs or through HTTP proxy 
using module proxy protocol. Detailed description about this mechanism can be found in Golang docs in 
section [Remote import path](https://golang.org/cmd/go/#hdr-Remote_import_paths). 

As described in [Golang docs](https://golang.org/cmd/go/#hdr-Module_proxy_protocol), go module proxy 
is any web server that can respond to GET requests of a specified form. 

Here is a full list of GET requests sent to a Go module proxy with corresponding examples:

##### Getting a list of known versions of the given module 
Request form: `GET $GOPROXY/<module>/@v/list`  
Response: list of the existing versions, one for line in the response body
```bash
curl -i -H -G https://proxy.golang.org/github.com/liujianping/ts/@v/list
HTTP/2 200 
accept-ranges: bytes
access-control-allow-origin: *
content-length: 49
content-type: text/plain; charset=UTF-8
date: Thu, 09 Apr 2020 06:44:59 GMT
expires: Thu, 09 Apr 2020 06:45:59 GMT
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
x-xss-protection: 0
cache-control: public, max-age=60
age: 39
alt-svc: quic=":443"; ma=2592000; v="46,43",h3-Q050=":443"; ma=2592000,h3-Q049=":443"; ma=2592000,h3-Q048=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,h3-T050=":443"; ma=2592000

v0.0.1
v0.0.5
v0.0.3
v0.0.4
v0.0.2
v0.0.6
v0.0.7
```
##### Getting json-formatted metadata for given version 
Request form: `GET $GOPROXY/<module>/@v/<version>.info`  
Response: .info file body in the response body
```bash
curl -i -H -G https://proxy.golang.org/github.com/liujianping/ts/@v/v0.0.7.info
HTTP/2 200 
accept-ranges: bytes
access-control-allow-origin: *
cache-control: public, max-age=10800
content-length: 50
content-type: application/json
date: Thu, 09 Apr 2020 06:47:14 GMT
expires: Thu, 09 Apr 2020 09:47:14 GMT
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
x-xss-protection: 0
alt-svc: quic=":443"; ma=2592000; v="46,43",h3-Q050=":443"; ma=2592000,h3-Q049=":443"; ma=2592000,h3-Q048=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,h3-T050=":443"; ma=2592000

{"Version":"v0.0.7","Time":"2019-06-28T10:22:31Z"}
```
##### Getting go.mod file for specified version 
Request form: `GET $GOPROXY/<module>/@v/<version>.mod`  
Response: .mod file body in the response body
```bash
curl -i -H -G https://proxy.golang.org/github.com/liujianping/ts/@v/v0.0.7.mod
HTTP/2 200 
accept-ranges: bytes
access-control-allow-origin: *
cache-control: public, max-age=10800
content-length: 281
content-type: text/plain; charset=UTF-8
date: Thu, 09 Apr 2020 06:47:52 GMT
etag: "6e2010ad8e07349f7e103b68ad02b0885033f4f39bf4bb5875bf5dc8b8add9d8"
expires: Thu, 09 Apr 2020 09:47:52 GMT
last-modified: Fri, 20 Sep 2019 19:26:17 GMT
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
x-xss-protection: 0
alt-svc: quic=":443"; ma=2592000; v="46,43",h3-Q050=":443"; ma=2592000,h3-Q049=":443"; ma=2592000,h3-Q048=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,h3-T050=":443"; ma=2592000

module github.com/liujianping/ts

go 1.12

require (
	github.com/araddon/dateparse v0.0.0-20190622164848-0fb0a474d195
	github.com/spf13/cobra v0.0.5
	github.com/spf13/viper v1.4.0
	github.com/stretchr/testify v1.3.0
	github.com/x-mod/build v0.1.0
	github.com/x-mod/errors v0.1.6
)
```
##### Getting zip archive for specified version 
Request form:`GET $GOPROXY/<module>/@v/<version>.zip`  
Response: .zip file
```bash
curl -i -H -G https://proxy.golang.org/github.com/liujianping/ts/@v/v0.0.7.zip
HTTP/2 200 
accept-ranges: bytes
access-control-allow-origin: *
cache-control: public, max-age=10800
content-length: 22758
content-type: application/zip
date: Thu, 09 Apr 2020 06:48:46 GMT
etag: "dfabe462193440509ad742f00e11940d86a1b3610734e4c50ec2386f9fc04d59"
expires: Thu, 09 Apr 2020 09:48:46 GMT
last-modified: Fri, 20 Sep 2019 19:26:17 GMT
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
x-xss-protection: 0
alt-svc: quic=":443"; ma=2592000; v="46,43",h3-Q050=":443"; ma=2592000,h3-Q049=":443"; ma=2592000,h3-Q048=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,h3-T050=":443"; ma=2592000

//binary data
```
##### Getting metadata about latest known version 
Request form: `GET $GOPROXY/<module>/@latest`  
Response: JSON-formatted metadata(.info file body) about the latest known version
```bash
curl -i -H -G https://proxy.golang.org/github.com/liujianping/ts/@latest
HTTP/2 200 
accept-ranges: bytes
access-control-allow-origin: *
cache-control: public, max-age=60
content-length: 50
content-type: application/json
date: Thu, 09 Apr 2020 06:53:25 GMT
expires: Thu, 09 Apr 2020 06:54:25 GMT
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
x-xss-protection: 0
alt-svc: quic=":443"; ma=2592000; v="46,43",h3-Q050=":443"; ma=2592000,h3-Q049=":443"; ma=2592000,h3-Q048=":443"; ma=2592000,h3-Q046=":443"; ma=2592000,h3-Q043=":443"; ma=2592000,h3-T050=":443"; ma=2592000

{"Version":"v0.0.7","Time":"2019-06-28T10:22:31Z"}
```

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
