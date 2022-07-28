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
allows to provide upload or download access for users and groups. Name of the repository configuration file is
the name of the repository.

For now Artipie supports the following repository types:

| Type  | Description   |
|---|---|
| [Files](./Configuration-Repository.md#file) | General purpose files repository |
| [Files Proxy](./Configuration-Repository.md#file-proxy-mirror) | Files repository proxy |
| [Maven](./Configuration-Repository.md#maven) | [Java artifacts and dependencies repository](https://maven.apache.org/what-is-maven.html) |
| [Maven Proxy](./Configuration-Repository.md#maven-proxy) | Proxy for maven repository |
| [Rpm](./Configuration-Repository.md#rpm) | `.rpm` ([linux binaries](https://rpm-packaging-guide.github.io/)) packages repository |
| [Docker](./Configuration-Repository.md#docker) | [Docker images registry](https://docs.docker.com/registry/) |
| [Docker Proxy](./Configuration-Repository.md#docker-proxy) | Proxy for docker repository |
| [Helm](./Configuration-Repository.md#helm) | [Helm charts repository](https://helm.sh/docs/topics/chart_repository/) |
| [Npm](./Configuration-Repository.md#npm) | [JavaScript code sharing and packages store](https://www.npmjs.com/) |
| [Npm Proxy](./Configuration-Repository.md#npm-proxy) | Proxy for NPM repository |
| [Composer](./Configuration-Repository.md#Composer) | [Dependency manager PHP packages](https://getcomposer.org/) |
| [NuGet](./Configuration-Repository.md#nuget) | [Hosting service for .NET packages](https://www.nuget.org/packages) |
| [Gem](./Configuration-Repository.md#gem) | [RubyGem hosting service](https://rubygems.org/) |
| [PyPI](./Configuration-Repository.md#pypi) | [Python packages index](https://pypi.org/) |
| [PyPI Proxy](./Configuration-Repository.md#pypi) | Proxy for Python repository |
| [Go](./Configuration-Repository.md#go) | [Go packages storages](https://golang.org/cmd/go/#hdr-Module_proxy_protocol) |
| [Debian](./Configuration-Repository.md#debian) | [Debian linux packages repository](https://wiki.debian.org/DebianRepository/Format) |
| [Conda](./Configuration-Repository.md#conda) | [Built packages for data science](https://www.anaconda.com/) |

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

## File

Files repository is a general purpose file storage which provides API for upload and download: `PUT` requests for upload and `GET` for download.
To setup this repository, create config with `file` repository type and storage configuration. Permissions configuration can authorize
users allowed to upload and download.

*Example:*
```yaml
repo:
  type: file
  storage: default
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```

In order to upload a binary file to the storage, send a `PUT` HTTP request with file contents:

```bash
echo "hello world" > test.txt
curl -X PUT --data-binary "@test.txt" http://{host}:{port}/{repository-name}/test.txt
```

In order to download a file, send a `GET` HTTP request:

```bash
curl -X GET http://{host}:{port}/{repository-name}/text.txt
```

In the examples above `{host}` and `{port}` Artipie service host and port, `{repository-name}` 
is the name of files repository.

## File proxy (mirror)

File proxy or mirror is a general purpose files mirror. It acts like a transparent HTTP proxy for one host
and caches all the data locally. To configure it use `file-proxy` repository type with required `remotes` section which should include
one remote configuration. Each remote config must provide `url` for remote file server and optional `username` and `password` for authentication.
Proxy is a read-only repository, nobody can upload to it. Only `download` permissions make sense here. Storage can be configured for
caching capabilities.

*Example:*
```yaml
repo:
  type: file-proxy
  storage: default
  remotes:
    - url: "https://remote-server.com"
      username: "alice" # optional username
      password: "qwerty" # optional password
      storage: # optional storage to cache proxy data
        type: fs
        path: tmp/files-proxy/data
  permissions:
    "*":
      - download
```

In order to download a file, send a `GET` HTTP request:

```bash
curl -X GET http://{host}:{port}/{repository-name}/test.txt
```
were `{host}` and `{port}` Artipie service host and port, `{repository-name}`
is the name of repository. Files proxy repository will proxy the request to remote, cache data in 
storage (if configured) and return the result.

