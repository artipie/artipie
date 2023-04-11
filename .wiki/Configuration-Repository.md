# Repository configuration

Artipie repository configuration is a yaml file, where repository type and artifacts storage are required
to be specified:
```yaml
repo:
  type: maven
  storage: 
    type: fs
    path: /tmp/artipie/data
```
`type` specifies the type of the repository (all supported types are listed below) and `storage` 
[configures](./Configuration-Storage) a storage to store repository data. Check [policy section](./Configuration-Policy)
and learn how to set permissions to upload or download from repository for users. 

> **Warning**  
> Name of the repository configuration file is the name of the repository.

# Supported repository types

For now Artipie supports the following repository types:

| Type                             | Description                                                                               |
|----------------------------------|-------------------------------------------------------------------------------------------|
| [Files](file)                    | General purpose files repository                                                          |
| [Files Proxy](file-proxy-mirror) | Files repository proxy                                                                    |
| [Maven](maven)                   | [Java artifacts and dependencies repository](https://maven.apache.org/what-is-maven.html) |
| [Maven Proxy](maven-proxy)       | Proxy for maven repository                                                                |
| [Rpm](rpm)                       | `.rpm` ([linux binaries](https://rpm-packaging-guide.github.io/)) packages repository     |
| [Docker](docker)                 | [Docker images registry](https://docs.docker.com/registry/)                               |
| [Docker Proxy](docker-proxy)     | Proxy for docker repository                                                               |
| [Helm](helm)                     | [Helm charts repository](https://helm.sh/docs/topics/chart_repository/)                   |
| [Npm](npm)                       | [JavaScript code sharing and packages store](https://www.npmjs.com/)                      |
| [Npm Proxy](npm-proxy)           | Proxy for NPM repository                                                                  |
| [Composer](composer)             | [Dependency manager for PHP packages](https://getcomposer.org/)                           |
| [NuGet](nuget)                   | [Hosting service for .NET packages](https://www.nuget.org/packages)                       |
| [Gem](gem)                       | [RubyGem hosting service](https://rubygems.org/)                                          |
| [PyPI](pypi)                     | [Python packages index](https://pypi.org/)                                                |
| [PyPI Proxy](pypi-proxy)         | Proxy for Python repository                                                               |
| [Go](go)                         | [Go packages storages](https://golang.org/cmd/go/#hdr-Module_proxy_protocol)              |
| [Debian](debian)                 | [Debian linux packages repository](https://wiki.debian.org/DebianRepository/Format)       |
| [Anaconda](anaconda)             | [Built packages for data science](https://www.anaconda.com/)                              |
| [HexPM](hexpm)                   | [Package manager for Elixir and Erlang](https://www.hex.pm/)                              |

Detailed configuration for each repository is provided in the corresponding subsection below.

## Single repository on port

Artipie repositories may run on separate ports if configured.
This feature may be especially useful for Docker repository,
as it's API is not well suited to serve multiple repositories on single port.

To run repository on its own port
`port` parameter should be specified in repository configuration YAML as follows:

```yaml
repo:
  type: <repository type>
  port: 54321
  ...
```

> **Warning**  
> Artipie scans repositories for port configuration only on start, 
> so server requires restart in order to apply changes made in runtime.
 
## Filters

Artipie provides means to filter out resources of a repository by specifying patterns of resource location.
The filtering patterns should be specified in ``filters`` section of YAML repository configuration.
Two list of filtering patterns can be specified inside ``filters`` section:
- ``include`` list of patterns of allowed resources.
- ``exclude`` list of patterns of forbidden resources.
```
filters:,
  include:
    ...
  exclude:
    ...
```
Each http-request to repository resource are controlled by filters.
The following rules are used to get access to repository resource:
- **Repository resource is allowed** if it matches at least one of patterns in the ``include`` list and does not match any of patterns in the ``exclude`` list.
- **Repository resource is forbidden** if it matches at least one of patterns in the ``exclude`` list or both list of patterns ``include`` and ``exclude`` are empty.

Artipie provides out-of-the-box following pattern matching types:
- **glob** patters specify sets of resource locations with wildcard characters. Description of platform specific glob syntax is [here](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
- **regexp** patters specify sets of resource locations with [regular expression syntax](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html)

Each of pattern matching types can be defined as separate YAML-section inside ``include`` and ``exclude`` sections:
``glob`` and ``regexp`` sections should contain list of filters corresponding type of patter syntax.
```
filters:,
  include:
    glob:
      - filter: '**/org/springframework/**/*.jar'
      - filter: '**/org/apache/logging/log4j/log4j-core/**/*.jar'
    regexp:
      - filter: '.*/com/artipie/.*\.jar'  
      - filter: '.*/com/artipie/.*\.zip\?([^&]+)&(user=M[^&]+).*'  
  exclude:
    glob:
      - filter: '**/org/apache/logging/log4j/log4j-core/2.17.0/*.jar'
    ...
```

Filters are ordered by definition order or/and priority. Each filter has a priority field that should contain numeric value of filter priority. The default value of priority is zero.

The usage of filter's priority allows to organize filters as ordered sequence of filters.
Internally filtering algorithm searches first matched filter in each list of filters(``include`` and ``exclude``) so usage of ordering can be useful here.

### Glob-filter
``Glob-filter`` uses path part of request for matching.

Yaml format:
- ``filter``: globbing expression. It is mandatory and value contains globbing expression for request path matching. The value should be quoted to be compatible with YAML-format.
- ``priority``: priority value. It is optional and provides priority value. Default value is zero priority.

### Regexp-filter
``Regexp-filter`` uses path part of request or full URI for matching.

Yaml format:
- ``filter``: regular expression. It is mandatory and value contains regular expression for request matching. The value should be quoted to be compatible with YAML-format.
- ``priority``: priority value. It is optional and provides priority value. Default is zero priority.
- ``full_uri``: is a ``Boolean`` value. It is optional with default value 'false' and implies to match with full URI or path part of URI.
- ``case_insensitive``: is a ``Boolean`` value. It is optional with default value 'false' and implies to ignore case in regular expression matching.

### Custom filter type
Custom filter types are supported by Artipie. To implement new filter type it is required following:
- Provide custom implementation of filter type by extending ``com.artipie.http.filter.Filter`` interface 
and to define custom filtering logic inside of method ``check(RequestLineFrom line, Iterable<Map.Entry<String, String>> headers)``. 
The Method ``check`` should return ``true`` if filter matches.
- Provide custom implementation of filter factory by extending ``com.artipie.http.filter.FilterFactory`` and to annotate by name of new filter type (like annotation values ``glob`` and ``regexp`` are used in ``GlobFilterFactory`` and ``RegexpFilterFactory`` accordingly).
This annotation's value should be specified as new pattern type inside ``include`` and ``exclude`` YAML-sections.
- Assemble jar-file and add it to Artipie's classpath

See examples of classes ``GlobFilter``, ``GlobFilterFactory``, ``RegexpFilter`` and ``RegexpFilterFactory`` in [com.artipie.http.filter](https://github.com/artipie/http/tree/master/src/main/java/com/artipie/http/filter) package.
 