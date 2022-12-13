## Metrics

Artipie metrics are meant to gather incoming HTTP requests and storage operations statistic and provide it in the 
[`Prometheus`](https://prometheus.io/) compatible format. Under the hood [Micrometer](https://micrometer.io/) is used
to gather the metrics.

Besides custom Artipie metrics, Vert.x embedded [Micrometer metrics](https://vertx.io/docs/3.9.13/vertx-micrometer-metrics/java/)
and [JVM and system metrics](https://micrometer.io/docs/ref/jvm) are provided.

To enable metrics, add section `metrics` to Artipie main configuration file:
```yaml
meta:
  metrics:
    endpoint: "/metrics/vertx" # Path of the endpoint, starting with `/`, where the metrics will be served
    port: 8087 # Port to serve the metrics
    types:
      - jvm # enables jvm-related metrics 
      - storage # enables storage-related metrics
      - http # enables http requests/responses related metrics
```

Both `endpoint` and `port` fields are required. If one of the fields is absent, metrics are considered as not enabled. 
Sequence `types` is optional: if `types` is absent all metrics are enabled, if it's present and empty, only
Vert.x embedded metrics are available. Add `types` items `jvm`, `storage` and/or `http` to enable required metrics. 

### Artipie metrics

Artipie gather the following metrics:

| Name                                | Type    | Description                           | Tags                  |
|-------------------------------------|---------|---------------------------------------|-----------------------|
| artipie_response_body_size_bytes    | summary | Response body size and chunks         | method, route         |
| artipie_request_body_size_bytes     | summary | Request body size and chunks          | method, route         |
| artipie_request_counter_total       | counter | Requests counter                      | method, route, status |
| artipie_response_send_seconds       | summary | Response.send execution time          | route                 |
| artipie_connection_accept_seconds   | summary | Connection.accept execution time      | route                 |
| artipie_slice_response_seconds      | summary | Slice.response execution time         | route                 |
| artipie_storage_value_seconds       | summary | Time to read value from storage       | id, key               |
| artipie_storage_value_size_bytes    | summary | Storage value size and chunks         | id, key               |
| artipie_storage_save_seconds        | summary | Time to save storage value            | id, key               |
| artipie_storage_exists_seconds      | summary | Storage exists operation time         | id, key               |
| artipie_storage_list_seconds        | summary | Storage list operation time           | id, key               |
| artipie_storage_move_seconds        | summary | Storage move operation time           | id, key, dest         |
| artipie_storage_metadata_seconds    | summary | Storage metadata operation time       | id, key               |
| artipie_storage_delete_seconds      | summary | Storage delete operation time         | id, key               |
| artipie_storage_deleteAll_seconds   | summary | Storage deleteAll operation seconds   | id, key               |
| artipie_storage_exclusively_seconds | summary | Storage exclusively operation seconds | id, key               |

All the metrics for storage operations report `error` events in the case of any errors, the events have `_error` postfix.

Tags description:

| Name   | Description                                                                                                                                              |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| method | Request method, upper cased                                                                                                                              |
| route  | Request route                                                                                                                                            |
| status | [Response status](https://github.com/artipie/http/blob/master/src/main/java/com/artipie/http/rs/RsStatus.java), string                                   |
| id     | Storage id, returned by [Storage.identifier()](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/Storage.java) method |
| key    | Storage operation key                                                                                                                                    |
| dest   | Destination key in the storage move operation                                                                                                            |
