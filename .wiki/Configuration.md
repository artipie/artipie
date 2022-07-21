The main Artipie configuration file is `/etc/artipie/artipie.yml`.
It contains server meta configuration, such as:
 - `layout` - `flat` or `org` string, not required, default value is `flat`;
 - `storage` - repositories definition storage config, required;
 - `credentials` - user [credentials config](./Configuration-Credentials.md);
 - `configs` - repository config files location, not required, the storage key relatively to the 
main storage, or, in file system storage terms, subdirectory where repo configs are located relatively to the storage;
 - `metrics` - enable and set [metrics collection](./Configuration-Metrics.md), not required

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

Layout specifies the URI path layout for Artipie. In case of `flat`,
Artipie provides repositories at first path level, e.g. `artipie.host/repo1`,
`artipie.host/repo2`. For `org` layouts URI path has two parts: `<org>/<repo>`,
where `<org>` is organisation name, and `<repo>` the name of repository,
e.g. `artipie.host/artipie/maven` - `maven` repository of `artipie` organisation.
In `org` layout, organisation may have a maintainer who can manage
repositories and permissions within organisation; the maintainer can add,
delete and edit repositories, add granular permissions for users for each repository.

Storage - is a [storage configuration](https://github.com/artipie/artipie/wiki/Configuration-Storage)
for [repository definitions](https://github.com/artipie/artipie/wiki/Configuration-Repository).
It locates a storage where all config files for each repository are located. Keep in mind,
Artipie user should have read and write permissions for this storage.

You may want configure it via environment variables:

- `SSL_TRUSTALL` - trust all unknown certificates
