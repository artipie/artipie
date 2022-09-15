## NPM Proxy

NPM proxy repository will proxy all the requests to configured remote and store all the downloaded
packages into configured storage:

```yaml
repo:
  type: npm-proxy
  path: {repository-name}
  storage:
    type: fs
    path: /var/artipie/data/
  settings:
    remote:
      url: http://npmjs-repo/
```

All the fields of YAML config are required, `path` is the repository relative path, [storage section](./Configuration-Storage)
configures storage to cache the packages, `settings` section sets remote repository url.

To use Artipie NPM proxy repository with `npm` client, specify the repository URL with `--registry` option:
```bash
npm install @hello/my-project-name --registry http://{host}:{port}/{repository-name}
```
or it's possible to set Artipie repository as a default registry:
```bash
npm set registry http://{host}:{port}/{repository-name}
```

In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file).