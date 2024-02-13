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
  storage: # optional storage to cache proxy data
    type: fs
    path: tmp/files-proxy/data
  http_client: # optional, settings for the HttpClient that will be used in xxx-proxy repositories
    connection_timeout: 25000 # optional, default 15000 ms 
    idle_timeout: 500 # optional, default 0
    trust_all: true # optional, default false
    follow_redirects: true # optional, default true
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
  remotes:
    - url: "https://remote-server.com"
      username: "alice" # optional username
      password: "qwerty" # optional password
     
```

In order to download a file, send a `GET` HTTP request:

```bash
curl -X GET http://{host}:{port}/{repository-name}/test.txt
```
where `{host}` and `{port}` Artipie service host and port, `{repository-name}`
is the name of repository. Files proxy repository will proxy the request to remote, cache data in
storage (if configured) and return the result.