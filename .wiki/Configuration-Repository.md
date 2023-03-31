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
