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