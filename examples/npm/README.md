### NPM Repo

Try this `npm.yaml` file:

```yaml
repo:
  type: npm
  path: /npm
  storage:
    type: fs
    path: /tmp/artipie/data/npm
  permissions:
    john:
      - download
      - upload
    jane:
      - upload
```

To publish your npm project use the following command:

```bash
$ npm publish --registry=http://localhost:8080/npm
```

### NPM Proxy Repo

Try this `npm-proxy.yaml` file:

```yaml
repo:
  type: npm-proxy
  path: npm-proxy
  storage:
    type: fs
    path: /tmp/artipie/data/npm-proxy
  settings:
    remote:
      url: https://registry.npmjs.org
```

To use it for downloading packages use the following command:

```bash
$ npm install --registry=http://localhost:8080/npm-proxy <package name>
```

or set it as a default repository:

```bash
$ npm set registry http://localhost:8080/npm-proxy
```