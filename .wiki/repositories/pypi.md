## PyPI

PyPI is a [Python Index Repository](https://pypi.org/), it allows to store and distribute python packages. 
Artipie supports this repository type:
```yaml
repo:
  type: pypi
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    "*":
      - download
    john:
      - upload
```
Check [storage](./Configuration-Storage.md) and [permission](./Configuration-Repository-Permissions.md)
documentations to learn more about these settings.

To publish the packages with [twine](https://packaging.python.org/tutorials/packaging-projects/#uploading-the-distribution-archives)
specify Artipie repository url with `--repository-url` option
```bash
$ twine upload --repository-url http://{host}:{port}/{repository-name} -u {username} -p {password} my-project/dist/*
```

To install the package with `pip install` specify Artipie repository url with `--index-url` option:

```bash
$ pip install --index-url http://{username}:{password}@{host}:{port}/{repository-name} my-project
```

In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file), 
`username` and `password` are credentials of Artipie user.