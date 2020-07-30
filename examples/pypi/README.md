### Python repo

![](https://github.com/artipie/artipie/workflows/Proof::pypi/badge.svg)

Try this `pypi.yaml` file:

```yaml
repo:
  type: my-pypi.yaml
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    "*":
      - "*"
```

To publish your packages with [twine](https://packaging.python.org/tutorials/packaging-projects/#uploading-the-distribution-archives) 
specify Artipie repository url with `--repository-url` option
```bash
$ python3 -m twine upload --repository-url http://localhost:8080/pypi/ -u username -p password myproject/dist/*
```

To install package with `pip install` specify Artipie repository url with `--index-url` option:

```bash
$ python -m pip install --index-url http://username:password@localhost:8080/pypi/ myproject
```