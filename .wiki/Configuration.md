## Main Artipie configuration

The main Artipie configuration is a `yaml` file, the file is required to start the service. 
Location of this file can be passed as a parameter to Java application (Artipie `jar` package) 
with `--config-file` (or short alternative `-f`) option. Artipie docker image default location 
of this file is `/etc/artipie/artipie.yml`. 

Yaml configuration file contains server meta configuration, such as:
 - `layout` - `flat` or `org` string, not required, default value is `flat`;
 - `storage` - repositories definition storage config, required;
 - `credentials` - user [credentials config](./Configuration-Credentials.md);
 - `configs` - repository config files location, not required, the storage key relative to the 
main storage, or, in file system storage terms, subdirectory where repo configs are located relatively to the storage;
 - `metrics` - enable and set [metrics collection](./Configuration-Metrics.md), not required.

Example: 
```yaml
# /etc/artipie/artipie.yml
meta:
  layout: org
  storage:
    type: fs
    path: /tmp/artipie/configs
  congigs: repo
  credentials:
    type: file
    path: _credentials.yml
  metrics:
    type: log
```

Layout specifies the URI path layout for Artipie. In case of `flat` layout,
Artipie provides repositories at first path level, e.g. `{host}:{port}/maven`,
`{host}:{port}/repo2`. For `org` layouts URI path has two parts: `<username>/<repo>`,
where `<username>` is the name of the user who owns the repository, and `<repo>` the name of repository,
e.g. `{host}:{port}/john/maven` - John's `maven` repository.
In `org` layout, repositories have a maintainer (owner) who can manage
repositories and permissions; the maintainer can add,
delete and edit repositories, add granular permissions for other users for each repository.

Storage - is a [storage configuration](./Configuration-Storage.md)
for [repository definitions](./Configuration-Repository.md).
It sets a storage where all config files for each repository are located. Keep in mind,
Artipie user should have read and write permissions for this storage.

Here is the example of Artipie configuration files structure for `flat` layout based on 
`/etc/artipie/artipie.yml` example above:
```
/tmp/artipie/configs
│   _storages.yaml
│   _credentials.yaml    
│
└───repo
│   │   my-maven.yml
│   │   main-python.yaml
│   │   docker-registry.yaml
```

If the layout is `org`, then repository configurations will be located in users' subdirectories:
```
/tmp/artipie/configs
│   _storages.yaml
│   _credentials.yaml    
│
└───repo
│   │───john
│   │   │   go-repo.yaml
│   │   │   docker-repo.yaml
│   │   │   _storages.yaml
│   │───mark
│   │   │   maven-repo.yaml
│   │   │   nuget-repo.yaml
```

In the examples above `_storages.yaml` is a file for [storages aliases](./Configuration-Storage.md#storage-aliases)
(note, that is the case of `org` layout it can be added for users individually) and
`_credentials.yaml` describes Artipie [users](./Configuration-Credentials.md). `repo` subdirectory
(as configured with `configs` field in `/etc/artipie/artipie.yml`) contains configs for repositories. If `configs` 
setting is omitted in `/etc/artipie/artipie.yml`, then repo configs will be located in `/tmp/artipie/configs`
directly.

Note that Artipie understands both extensions: `yml` and `yaml`.

## Additional configuration 

Here is a list of some additional configurations:

- To configure port for Artipie server use `--port` (or short alternative `-p`) option, default port is 80
- Set environment variable `SSL_TRUSTALL` to trust all unknown certificates
