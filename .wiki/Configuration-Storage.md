# Storage

Artipie "Storage" is an abstraction on top of multiple key-value storage providers. Artipie supports:
 - file system storage
 - S3 storage
 - etcd storage (see limitations)
 - in-memory
 - custom storage

The Storage is used for storing repository data, proxy/mirrors repository caching and for Artipie 
configuration. Such storage can be configured in various config files and in various sections of 
config files, but storage configuration structure is always the same: each storage is defined by 
`storage` yaml key, mandatory `type` parameter and provider dependent configuration parameters.

## File System storage

The file system storage uses file-system as a back-end for binary key-value mapping - it save blobs 
in files using file paths as a keys. It requires the root path to be configured with `path` 
parameters.

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

## In memory storage

In-memory storage is not persistent, it exists only while Artipie process is alive and is used in 
Artipie tests, check the [implementation](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/memory/InMemoryStorage.java) 
for more details. There is no possibility to use in memory storage from configuration, 
it's for unit and integration tests only.

## Custom storage

Artipie users have an option to implement and use a custom storage.
If you want to make your storage, you need to define `asto-code` dependency in `pom` file of your project:
```xml
<dependency>
   <groupId>com.artipie</groupId>
   <artifactId>asto-core</artifactId>
   <version>...</version>
</dependency>
```
On the next step, you have to implement interface 
[Storage](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/Storage.java)
to host data in the place which you need. The interface 
[StorageFactory](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/factory/StorageFactory.java)
is responsible for creating a new storage instance. You have to implement this interface and 
mark the implementation with annotation [ArtipieStorageFactory](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/factory/ArtipieStorageFactory.java). 
This annotation helps Artipie to find factory classes and provides a name of storage type. 
Storage type name must be unique in the scope of one Artipie server. In the case of type name conflict, 
Artipie will throw an exception on a start-up stage. Storage configuration is represented by an 
interface [StorageConfig](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/factory/StorageConfig.java). 
Currently, this interface has the single implementation 
[YamlStorageConfig](https://github.com/artipie/asto/blob/master/asto-core/src/main/java/com/artipie/asto/factory/StorageConfig.java#L96) 
that allows to define configuration as `yaml` file. It's also possible to use an own implementation of `StorageConfig`.

To start Artipie with a custom storage, you have to:
- provide a file with configuration of storage; this is done the same way as for other storage types.
- put a jar file that contains implementation classes and all needed libraries to classpath.

If logging is switched to `info` level, you should see the following log record:
```
Initiated storage factory [type={your-storage-type}, class={your-storage-factory-class-name}]
```

You can study [a storage implementation based on Redis java client Redisson](https://github.com/artipie/asto/tree/master/asto-redis/src/main/java/com/artipie/asto/redis)
as a good example.

# Storage Aliases

Artipie has special configuration item for storage aliases: `_storages.yaml` file located in configuration root.
This file can define storages with names, then repository configuration file can use these names (or aliases) 
instead of full storage description. It allows to avoid duplicates and to hide real storage configuration or 
credentials from repository maintainers:
Artipie server administrator can configure storage and provide alias to users, and users will set 
this alias for repositories instead of full configuration.
```yaml
# _storages.yaml
storages:
  default: # storage alias name to use in repository configs
    type: fs
    path: /var/artipie/data
  remote_s3: # storage alias name to use in repository configs
     type: s3
     bucket: artipie
     region: east
     endpoint: https://minio.selfhosted/s3
     credentials:
        type: basic
        accessKeyId: asagn8as8f81
        secretAccessKey: 9889sg8nas8ng
```
```yaml
# repository configuration
repo:
  type: file
  storage: default
# or
repo:
   type: maven
   storage: remote_s3
```
