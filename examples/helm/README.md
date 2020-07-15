### Helm chart repo

![](https://github.com/artipie/artipie/workflows/Proof::helm/badge.svg)

Try this `helm.yaml` file:

```yaml
repo:
  type: helm
  storage:
    type: fs
    path: /tmp/artipie/data/helm-charts
```

Publish a chart:

```bash
$ curl --data-binary "@my_chart-1.6.4.tgz" http://localhost:8080/helm
```

Install a chart:

```bash
$ helm repo add artipie http://localhost:8080/helm/charts
$ helm install my_chart artipie
```