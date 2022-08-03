# Repository configuration

Artipie repository configuration is a yaml file, where repository type and artifacts storage are required
to be specified:
```yaml
repo:
  type: maven
  storage: 
    type: fs
    path: /tmp/artipie/data
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```
`type` specifies the type of the repository (all supported types are listed below) and `storage` 
[configures](./Configuration-Storage.md) a storage to store repository data. [Permissions section](./Configuration-Repository Permissions.md)
allows to provide upload or download access for users and groups. 

> **Warning**  
> Name of the repository configuration file is the name of the repository.

For now Artipie supports the following repository types:

| Type  | Description   |
|---|---|
| [Files](./repositories/file.md) | General purpose files repository |
| [Files Proxy](./repositories/file-proxy-mirrow.md) | Files repository proxy |
| [Maven](./repositories/maven.md) | [Java artifacts and dependencies repository](https://maven.apache.org/what-is-maven.html) |
| [Maven Proxy](./repositories/maven-proxy.md) | Proxy for maven repository |
| [Rpm](./repositories/rpm.md) | `.rpm` ([linux binaries](https://rpm-packaging-guide.github.io/)) packages repository |
| [Docker](./repositories/docker.md) | [Docker images registry](https://docs.docker.com/registry/) |
| [Docker Proxy](./repositories/docker-proxy.md) | Proxy for docker repository |
| [Helm](./repositories/helm.md) | [Helm charts repository](https://helm.sh/docs/topics/chart_repository/) |
| [Npm](./repositories/npm.md) | [JavaScript code sharing and packages store](https://www.npmjs.com/) |
| [Npm Proxy](./repositories/npm-proxy.md) | Proxy for NPM repository |
| [Composer](./repositories/composer.md) | [Dependency manager PHP packages](https://getcomposer.org/) |
| [NuGet](./repositories/nuget.md) | [Hosting service for .NET packages](https://www.nuget.org/packages) |
| [Gem](./repositories/gem.md) | [RubyGem hosting service](https://rubygems.org/) |
| [PyPI](./repositories/pypi.md) | [Python packages index](https://pypi.org/) |
| [PyPI Proxy](./repositories/pypi-proxy.md) | Proxy for Python repository |
| [Go](./repositories/go.md) | [Go packages storages](https://golang.org/cmd/go/#hdr-Module_proxy_protocol) |
| [Debian](./repositories/debian.md) | [Debian linux packages repository](https://wiki.debian.org/DebianRepository/Format) |
| [Anaconda](./repositories/anaconda.md) | [Built packages for data science](https://www.anaconda.com/) |

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
