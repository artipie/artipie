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
  http_client: # optional, settings for the HttpClient that will be used in xxx-proxy repositories
    connection_timeout: 25000 # optional, default 15000 ms 
    idle_timeout: 500 # optional, default 0
    trust_all: true # optional, default false
    follow_redirects: true #optional, default true
    http3: true # optional, default false
    jks: # optional
      path: /var/artipie/keystore.jks
      password: secret
    proxies:
      - url: http://proxy1.com
      - url: https://proxy2.com
        # the HTTP "Basic" authentication defined in RFC 2617
        realm: user_realm # if this field is defined, then `username` and `password` are mandatory
        username: user_name
        password: user_password
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