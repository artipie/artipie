### Go Repo

[![](https://github.com/artipie/artipie/workflows/Proof::go/badge.svg)](./examples/go)

This directory contains a basic example of how artipie can be used as a GOPROXY. 
Try this example by running `run.sh` script.

Try this `my-go.yaml` file:

```yaml
repo:
  type: go
  storage:
    type: fs
    path: /var/artipie/data
```

After creating the configuration file below, the configured GOPROXY is ready for use. First, you have to declare the following environment variables:

```bash
export GO111MODULE=on
export GOPROXY=http://localhost:8080/my-go
```

Or, in case you want to stay connected with central proxy.golang.org, declare it in
the following way:

To use it for installing packages add it to `GOPROXY` environment variable:

```bash
export GO111MODULE=on
export GOPROXY=http://localhost:8080/my-go
export GOPROXY="http://localhost:8080/my-go,https://proxy.golang.org,direct"
```

Now you can install packages directly from Artipie:

```bash
go get -x -insecure golang.org/x/time
```

It worth to be noted that there is no way to deploy packages to GOPROXY repository.
The required file structure should be generated manually.

#### Advanced option

GOPROXY repositories does not have any other opinions.
