# Artifacts metadata

Artipie can gather uploaded artifacts metadata and write them into [SQLite](https://www.sqlite.org/index.html) database.
To enable this mechanism, add the following section into Artipie main configuration file:

```yaml
meta:
  artifacts_database:
    sqlite_data_file_path: /var/artipie/artifacts.db
    threads_count: 2 # optional, default 1
    interval_seconds: 3 # optional, default 1
```

The essential here is `artifacts_database` section, other fields are optional. If `sqlite_data_file_path` field is absent,
a database file will be created at the parent location (directory) of the main configuration file. The metadata gathering
mechanism uses [quartz](http://www.quartz-scheduler.org/) scheduler to process artifacts metadata under the hood. Quartz
can be [configured separately](http://www.quartz-scheduler.org/documentation/quartz-2.1.7/configuration/ConfigMain.html),
by default it uses `org.quartz.simpl.SimpleThreadPool` with 10 threads. If `threads_count` is larger than thread pool size,
threads amount is limited to the thread pool size.

The database has only one table with the following structure:

| Name         | Type     | Description                              |
|--------------|----------|------------------------------------------|
| id           | bigint   | Unique identification, primary key       |
| repo_type    | char(10) | Repository type (maven, docker, npm etc) |
| repo_name    | char(20) | Repository name                          |
| name         | varchar  | Artifact full name                       |
| version      | varchar  | Artifact version                         |
| size         | bigint   | Artifact size                            |
| created_date | datetime | Date uploaded                            |
| owner        | varchar  | Artifact uploader login                  |

All the fields are not null, unique constraint is created on repo_name, name and version.

## Maven, NPM and PyPI proxy adapters

[Maven-proxy](maven-proxy), [npm-proxy](npm-proxy) and [python-proxy](pypi-proxy) have some extra mechanism to process
uploaded artifacts from origin repositories. Generally, the mechanism is a quartz job which verifies uploaded and 
saved to cash storage artifacts and adds metadata common mechanism and database. Proxy adapters metadata gathering is
enabled when artifacts database is enabled and proxy repository storage is configured. 
It's possible to configure `threads_count` and `interval_seconds` for[Maven-proxy](maven-proxy), [npm-proxy](npm-proxy) 
and [python-proxy](pypi-proxy) repositories individually.
Just add these fields into repository setting file, for example:
```yaml
repo:
  type: maven-proxy
  storage:
    type: fs
    path: /tmp/artipie/maven-central-cache
  threads_count: 3 # optional, default 1
  interval_seconds: 5 # optional, default 1
```
