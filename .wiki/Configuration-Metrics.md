## Metrics

You may enable some basic metrics collecting and periodic publishing to application log
by adding `metrics` to `meta` section of global configuration file `/etc/artipie/artipie.yml`:

```yaml
meta:
  metrics:
    type: log # Metrics type, for now only `log` type is supported
    interval: 5 # Publishing interval in seconds, default value is 5
```

To collect metrics via `Prometheus`, simply configure `metrics` like this :

```yaml
meta :
  metrics :
    type : prometheus
```