### Python repo

![](https://github.com/artipie/artipie/workflows/Proof::python/badge.svg)

Try this `pypi.yaml` file:

```yaml
repo:
  type: pypi
  storage:
    type: fs
    path: /tmp/artipie/data/python-repo
```

Publish a package(whl or tar.gz):
  * Install twine utility, if you don't do it already [docs](https://packaging.python.org/tutorials/packaging-projects/#uploading-the-distribution-archives).
```bash
$ python3 -m pip install --user --upgrade twine
```
  * build the package, as described in python docs
  * upload to server with a command
```bash
$ python3 -m twine upload --repository-url http://localhost:8080/pypi/ -u user.name -p pass testpkg/dist/*
```

Install a package:

```bash
$ pip install --index-url http://localhost:8080/pypi/ testpkg
```