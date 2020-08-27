### Helm chart repo

![](https://github.com/artipie/artipie/workflows/Proof::helm/badge.svg)

Try this `example_helm_repo.yaml` file:

```yaml
repo:
  path: "http://localhost:8080/example_helm_repo/"
  type: helm
  storage:
    type: fs
    path: /var/artipie/data
```

Publish a chart:

```bash
$ curl --data-binary "@my_chart-1.6.4.tgz" http://localhost:8080/example_helm_repo/
```

Install a chart:

```bash
$ helm repo add artipie http://localhost:8080/example_helm_repo/
$ helm install my_chart artipie
```