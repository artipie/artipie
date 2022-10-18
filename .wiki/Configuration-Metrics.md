## Metrics

// @todo #1031:30min Verify this configuration section: start Artipie locally, configure metrics (try 
//  each type) and check how they work. Check `com.artipie.metrics` package for all details. When checking,
//  extend this documentation with examples and details about gathered statistics.

Artipie metrics are meant to gather incoming HTTP requests statistic and publish it in the 
configured format: it can be application log, [`Prometheus`](https://prometheus.io/) or/and data storage.

Also, you can enable Vert.x embedded [Micrometer metrics](https://vertx.io/docs/3.9.13/vertx-micrometer-metrics/java/)
to obtain HTTP server statistics in [`Prometheus`](https://prometheus.io/) compatible format.

Section `metrics` of Artipie main configuration file is a yaml sequence, each item enables one of the
metrics. Thus, several metrics can be enabled in the same time.

### Logging metrics

To enable some basic metrics collecting and periodic publishing to application log
add `metrics` to `meta` section of global configuration file `/etc/artipie/artipie.yml`:

```yaml
meta:
  metrics:
    - 
      type: log # Metrics type, `log` to print statistics into application log
      interval: 5 # Publishing interval in seconds, default value is 5
```

### Storage metrics

Storage metrics will periodically publish statistics to [storage](./Configuration-Storage) as text files, 
here is the way to configure such metrics:
```yaml
meta:
  metrics:
    - 
      type: asto # Metrics type, `asto` to publish statistics into storage
      interval: 5 # Publishing interval in seconds, default value is 5
      storage: # Storage to publish the metrics to
        type: fs
        path: /tmp/artipie/statistict
```

Storage metrics can be obtained via HTTP `GET` request by path `http://{host}:{port}/.metrics`, 
where `{host}` and `{port}` Artipie service host and port accordingly. The response is a json object
with gathered statistics.

### Prometheus metrics

To collect metrics via [`Prometheus`](https://prometheus.io/), simply configure `metrics` like this:

```yaml
meta:
  metrics:
    - 
      type: prometheus
```
Metrics in Prometheus compatible format will be available on `http://{host}:{port}/prometheus/metrics` path,
where `{host}` and `{port}` Artipie service host and port. 

### Vertx server metrics

To enable Vert.x embedded [Micrometer metrics](https://vertx.io/docs/3.9.13/vertx-micrometer-metrics/java/) 
add the following configuration:
```yaml
meta:
  metrics:
    - 
      type: vertx # Metrics type, `vertx` for Vert.x metrics
      endpoint: "/metrics/vertx" # Path of the endpoint, starting with `/`, where the metrics will be served
      port: 8087 # Port to serve the metrics
```
The metrics will be served on specified port and endpoint path. Vert.x gather various HTTP server statistics,
such as number of opened connections, number of bytes received/sent by the server, number of 
requests being processed etc. Full list is available [here](https://vertx.io/docs/3.9.13/vertx-micrometer-metrics/java/#_http_server).
Statistics is served in [`Prometheus`](https://prometheus.io/) compatible format.
