## Helm

[Helm charts repository](https://helm.sh/docs/topics/chart_repository/) is a location where packaged 
charts can be stored and shared. Here is the configuration example for Helm repository:
```yaml
repo:
  type: helm
  url: http://{host}:{port}/{repository-name}
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    "*":
      - download
```

The repository configuration requires `url` field that contains repository full URL,
`{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file). Check
[storage](./Configuration-Storage) and [permission](./Configuration-Repository-Permissions)
documentations to learn more about these settings.

The chart can be published with simple HTTP `PUT` request:

```bash
$ curl --data-binary "@my_chart-1.6.4.tgz" http://{host}:{port}/{repository-name}/my_chart-1.6.4.tgz
```

To install a chart with `helm` command line tool use the following commands:
```bash
# add new repository
$ helm repo add {repo-name} http://{host}:{port}/{repository-name}
# install chart
$ helm install my_chart {repo-name}
```
`{repo-name}` is the name of the repository for `helm` command line tool, use any name that is convenient.
