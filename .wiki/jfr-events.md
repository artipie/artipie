## Artipie`s Java Flight Recorder Events
 
Java Flight Recorder (JFR) is a tool for collecting diagnostic and profiling data about a running Java application. 
It is integrated into the Java Virtual Machine (JVM) and causes almost no performance overhead, so it can be used 
even in heavily loaded production environments. When default settings are used, both internal testing and customer 
feedback indicate that performance impact is less than one percent. For some applications, it can be significantly 
lower. However, for short-running applications (which are not the kind of applications running in production 
environments), relative startup and warmup times can be larger, which might impact the performance by more than 
one percent. JFR collects data about the JVM as well as the Java application running on it.  
To get more information about JFR, please read [Oracle's Guide](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm#JFRUH170).

## Artipie`s Events
Artipie generates the following events:

### artipie.SliceResponse
Label: Slice Response  
Description: Event triggered when Artipie processes an HTTP request  
Category: Artipie

| Attribute      | Type   | Label                      |
|----------------|--------|----------------------------|
| startTime      | long   | Start Time (Timestamp)     |
| duration       | long   | Duration (Timespan)        |
| endTime        | long   | End Time (Timestamp)       |
| eventThread    | String | Event Thread               |
| method         | String | Request Method             |
| path           | String | Request Path               |
| headers        | String | Headers                    |
| requestChunks  | int    | Request Body Chunks Count  |
| requestSize    | long   | Request Body Value Size    |
| responseChunks | int    | Response Body Chunks Count |
| responseSize   | long   | Response Body Value Size   |

### artipie.StorageCreate
Label: Storage Create  
Description: Event triggered when storage is created  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |

### artipie.StorageSave
Label: Storage Save  
Description: Save value to a storage  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |
| chunks      | int    | Chunks Count           |
| size        | long   | Value Size             |

### artipie.StorageExists
Label: Storage Exists  
Description: Does a record with this key exist?  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |

### artipie.StorageValue
Label: Storage Get  
Description: Get value from a storage  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |
| chunks      | int    | Chunks Count           |
| size        | long   | Value Size             |

### artipie.StorageDelete
Label: Storage Delete  
Description: Delete value from a storage  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |

### artipie.StorageDeleteAll
Label: Storage Delete All  
Description: Delete all values with key prefix from a storage  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |

### artipie.StorageMove
Label: Storage Move  
Description: Move value from one location to another  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |
| target      | String | Target Key             |

### artipie.StorageList
Label: Storage List  
Description: Get the list of keys that start with this prefix  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |
| keysCount   | String | Key                    |

### artipie.StorageMetadata
Label: Storage Metadata  
Description: Get content metadata  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |

### artipie.StorageExclusively
Label: Storage Exclusively  
Description: Runs operation exclusively for specified key  
Category: Artipie, Storage

| Attribute   | Type   | Label                  |
|-------------|--------|------------------------|
| startTime   | long   | Start Time (Timestamp) |
| duration    | long   | Duration (Timespan)    |
| endTime     | long   | End Time (Timestamp)   |
| eventThread | String | Event Thread           |
| storage     | String | Storage Identifier     |
| key         | String | Key                    |


## Start docker container with JFR
Artipie's docker image provides the environment variable `JVM_ARGS` which can be used
to start and configure a recording from 
[the command line options](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/run.htm#JFRUH176).
For example:
```bash
docker run -it \
    -p 8080:8080 \
    -p 8086:8086 \
    -e JVM_ARGS="-XX:StartFlightRecording:filename=/var/artipie/prof_01.jfr" \
    -v /Users/username/artipie/jfr:/var/artipie \
    artipie/artipie:latest
```
It'll start a new Docker container with latest Artipie version, the command includes mapping of two
ports: on port `8080` repositories are served and on port `8086` Artipie Rest API and Swagger
documentation is provided. The environment variable `JVM_ARGS` defines the `-XX:StartFlightRecording` 
option of the java command, when starting the application. Recording will save data to the file `/var/artipie/prof_01.jfr`.
To persist this file on the machine that hosted container, we should define a 
[docker volume](https://docs.docker.com/storage/volumes/) using key `-v`.