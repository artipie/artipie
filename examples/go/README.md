### Go Repo

![](https://github.com/artipie/artipie/workflows/Proof::go/badge.svg)

Try this `go.yaml` file:

```yaml
repo:
  type: go
  path: go
  storage:
    type: fs
    path: /tmp/artipie/data/go
```

To use it for installing packages add it to `GOPROXY` environment variable:

```bash
$ export GOPROXY="http://localhost:8080/go,https://proxy.golang.org,direct"
```

Go packages have to be located in the local repository by their
names and versions, contain Go module and dependencies information
in `.mod` and `.info` files. Here is an example for package
`example.com/foo/bar` versions `0.0.1` and `0.0.2`:

```
/example.com
  /foo
    /bar
      /@v
        list
        v0.0.1.zip
        v0.0.1.mod
        v0.0.1.info
        v0.0.2.zip
        v0.0.2.mod
        v0.0.2.info
```

`list` is simple text file with list of the available versions.
You can use [go-adapter](https://github.com/artipie/go-adapter#how-it-works)
to generate necessary files and layout for Go source code.
