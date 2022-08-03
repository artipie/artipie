## NPM

NPM repository is the [repository for JavaScript](https://www.npmjs.com/) code sharing, packages
store and management. Here is the configuration example for NPM repository:

```yaml
repo:
  type: npm
  url: http://{host}:{port}/{repository-name}
  storage:
    type: fs
    path: /var/artipie/data/
  permissions:
    "*":
      - download
```

The NPM repository configuration requires `url` field that contains repository full URL,
`{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file). Check
[storage](../Configuration-Storage.md) and [permission](../Configuration-Repository%20Permissions.md)
documentations to learn more about these settings.

To use NPM repository with `npm` client, you can specify Artipie NPM repository with `--registry` option:
```bash
# to install the package
npm install @hello/my-project-name --registry http://{host}:{port}/{repository-name}
# to publish the package
npm publish @hello/my-project-name --registry http://{host}:{port}/{repository-name}
```
or it's possible to set Artipie as a default registry:
```bash
npm set registry http://{host}:{port}/{repository-name}
```