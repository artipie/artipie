### Python repo

![](https://github.com/artipie/artipie/workflows/Proof::pypi/badge.svg)

Try this `pypi.yaml` file:

```yaml
repo:
  type: pypi
  storage:
    type: fs
    path: /var/artipie/data
```

To publish your packages with [twine](https://packaging.python.org/tutorials/packaging-projects/#uploading-the-distribution-archives) 
specify Artipie repository url with `--repository-url` option
```bash
$ twine upload --repository-url http://localhost:8080/pypi/ -u username -p password myproject/dist/*
```

To install package with `pip install` specify Artipie repository url with `--index-url` option:

```bash
$ pip install --index-url http://username:password@localhost:8080/pypi/ myproject
```

### Python proxy

Try this `pypi-proxy.yaml` file to host a proxy to `https://pypi.org/simple/` repository:

```yaml
repo:
  type: maven-proxy
  remotes:
    - url: https://pypi.org/simple/
      username: Aladdin # optional
      password: OpenSesame # optional
      cache:
        storage:
          type: fs
          path: /tmp/artipie/data/my-pypi-cache
```

Artipie will redirect all requests to pypi simple repository.

Proxy repository supports caching in local storage, all previously accessed indexes and packages are 
available when source repository is down.
