## Go

Go repository is the storage for Go packages, it supports 
[Go Module Proxy protocol](https://golang.org/cmd/go/#hdr-Module_proxy_protocol). 
Here is the configuration example:
```yaml
repo:
  type: go
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    "*":
      - download
```
Check [storage](../Configuration-Storage.md) and [permission](../Configuration-Repository-Permissions.md)
documentations to learn more about these settings.

In order to use Artipie Go repository, declare the following environment variables:

```bash
export GO111MODULE=on
export GOPROXY="http://{host}:{port}/{repository-name}"
export GOSUMDB=off
# the next property is useful if SSL is not configured
export "GOINSECURE={host}*"
```

Now the package can be installed with the command:

```bash
go get -x golang.org/x/time
```
In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file).

There is no way to deploy packages to Artipie Go repository for now.