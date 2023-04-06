## File proxy (mirror)

File proxy or mirror is a general purpose files mirror. It acts like a transparent HTTP proxy for one host
and caches all the data locally. To configure it use `file-proxy` repository type with required `remotes` section which should include
one remote configuration. Each remote config must provide `url` for remote file server and optional `username` and `password` for authentication.
Proxy is a read-only repository, nobody can upload to it. Storage can be configured for
caching capabilities.

*Example:*
```yaml
repo:
  type: file-proxy
  storage: default
  remotes:
    - url: "https://remote-server.com"
      username: "alice" # optional username
      password: "qwerty" # optional password
      storage: # optional storage to cache proxy data
        type: fs
        path: tmp/files-proxy/data
```

In order to download a file, send a `GET` HTTP request:

```bash
curl -X GET http://{host}:{port}/{repository-name}/test.txt
```
where `{host}` and `{port}` Artipie service host and port, `{repository-name}`
is the name of repository. Files proxy repository will proxy the request to remote, cache data in
storage (if configured) and return the result.