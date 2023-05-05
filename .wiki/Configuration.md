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

## Additional configuration 

Here is a list of some additional configurations:

- To configure port for Artipie server use `--port` (or short alternative `-p`) option, default port is 80
- Set environment variable `SSL_TRUSTALL` to trust all unknown certificates
