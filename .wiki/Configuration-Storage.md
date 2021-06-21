# Storage

Artipie "Storage" is an abstraction on top of multiple key-value storage providers. Artipie supports:
 - file system storage
 - S3 storage
 - etcd storage (see limitations)
 - in-memory

The Storage is used for storing repository data, proxy/mirrors repository caching and for Artipie configuration. In configuration each storage
is defined by `storage` yaml key, mandatory `type` parameter and provider dependent configuration parameters.

## File System storage

The file system storage uses file-system as a back-end for binary key-value mapping - it save blobs in files using file paths as a keys.
It requires the root path to be configured with `path` parameters.

*Example:*
```yaml
storage:
  type: fs
  path: /var/artipie
```

## S3 storage

Artipie supports any S3-compatible cloud storage (e.g. AWS, Digital-Ocean, GCE). The type of S3 storage is `s3`.
Supported settings for S3 storage are:
 - `bucket` (string, **required**) - bucket name
 - `region` (string, optional) - bucket region name
 - `endpoint` (string, optional) - S3 API provider URL, default is standard AWS S3 endpoint 
 - `credentials` (map, **required**):
   - `type` (string, **required**) - authentication type, one of: `basic`
   - `accessKeyId` (string, **required**) - access API key ID
   - `secretAccessKey` (string, **required**) - secret key

*Example:*
```yaml
storage:
  type: s3
  bucket: artipie
  region: east
  endpoint: https://minio.selfhosted/s3
  credentials:
    type: basic
    accessKeyId: asagn8as8f81
    secretAccessKey: 9889sg8nas8ng
```

## In memory storage

In-memory storage is not persistent, it exists only while Artipie process is alive.
It's not recommended to use it in production, it may be helpful for debugging and testing.

*Example:*
```yaml
storage:
  type: in-memory
```

## Etcd storage

Etcd storage uses etcd cluster as a back-end. It may be useful for configuration storage of Artipie server.
This kind of storage require blobs to be smaller than 10Mb. Storage type is `etcd`, other parameters are:
 - `endpoints` (string list, **required**) - the list of valid cluster endpoints
 - `timeout` (number, optional) - connection timeout in milliseconds

*Example:*
```yaml
storage:
  type: etcd
  endpoints:
    - http://node1.ectd.local:2379
    - http://node2.etcd.local:2379
  timeout: 5000
```

## Aliases

Artipie has special configuration item for storage aliases: `_storages.yaml` located in configuration root.
This file can define storages with names, then repository users can use alias instead of storage configuration.
Also, it allows to hide real storage configuration or credentials from repository or organization maintainers:
Artipie server administrator can configure storage and provide alias to users, and users will set this alias
for repositories instead of configuration.
```yaml
# _storages.yaml
storages:
  default:
    type: fs
    path: /var/artipie/data
```
```
# repository configuration
repo:
  type: file
  storage: default
```
