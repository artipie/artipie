## Main Artipie configuration

The main Artipie configuration is a `yaml` file, the file is required to start the service. 
Location of this file can be passed as a parameter to Java application (Artipie `jar` package) 
with `--config-file` (or short alternative `-f`) option. Artipie docker image default location 
of this file is `/etc/artipie/artipie.yml`. 

Yaml configuration file contains server meta configuration, such as:
 - `storage` - repositories definition storage config, required;
 - `credentials` - user [credentials config](./Configuration-Credentials);
 - `configs` - repository config files location, not required, the storage key relative to the 
main storage, or, in file system storage terms, subdirectory where repo configs are located relatively to the storage;
 - `metrics` - enable and set [metrics collection](./Configuration-Metrics), not required.

Example: 
```yaml
# /etc/artipie/artipie.yml
meta:
  storage:
    type: fs
    path: /tmp/artipie/configs
  configs: repo
  # optional, settings for the HttpClient that will be used in xxx-proxy repositories
  http_client:
    # all fields are optional
    connection_timeout: 25000
    idle_timeout: 500
    trust_all: true
    follow_redirects: true
    http3: true
    jks:
      path: /var/artipie/keystore.jks
      password: secret
    proxies:
      - url: http://proxy1.com
      - url: https://proxy2.com
        # the HTTP "Basic" authentication defined in RFC 2617
        realm: user_realm
        username: user_name
        password: user_password
  credentials:
    - type: artipie
    - type: env
  policy:
    type: artipie
    storage:
      type: fs
      path: /tmp/artipie/security
  metrics:
    endpoint: "/metrics/vertx"
    port: 8087
```

Artipie provides repositories at first path level, e.g. `{host}:{port}/maven`, `{host}:{port}/test-pypi`.

Storage - is a [storage configuration](./Configuration-Storage)
for [repository definitions](./Configuration-Repository).
It sets a storage where all config files for each repository are located. Keep in mind,
Artipie user should have read and write permissions for this storage.

Here is the example of Artipie configuration files structure based on 
`/etc/artipie/artipie.yml` example above:
```
/tmp/artipie/configs
│   _storages.yaml 
│
└───repo
│   │   my-maven.yml
│   │   main-python.yaml
│   │   docker-registry.yaml
```

In the examples above `_storages.yaml` is a file for [storages aliases](./Configuration-Storage#storage-aliases),
file name for the aliases is fixed and should be `_storages` as shown above,
`repo` subdirectory (as configured with `configs` field in `/etc/artipie/artipie.yml`) contains configs for 
repositories. If `configs` setting is omitted in `/etc/artipie/artipie.yml`, then repo configs will be located 
in `/tmp/artipie/configs` directly.

Credentials and policy sections are responsible for [user credentials](./Configuration-Credentials) and [security policy](./Configuration-Policy).

Note that Artipie understands both extensions: `yml` and `yaml`.

### Http client settings
The http client settings can be overridden in `xxx-proxy` repository config.
Proxy servers can be defined by system properties as it's described in [Java documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html).
Default values:
 - connection_timeout: 15_000
 - idle_timeout: 0 
 - trust_all: false
 - follow_redirects: true
 - http3: false

## Additional configuration 

Here is a list of some additional configurations:

- To configure port for Artipie server use `--port` (or short alternative `-p`) option, default port is 80
- Set environment variable `SSL_TRUSTALL` to trust all unknown certificates
