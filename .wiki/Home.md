# Artipie

To get started with Artipie you need to understand your needs first.
Artipie is not just a binary artifact web server -- it's artifact management
constructor which consists of many components built into server assembly.

You have three options for working with Artipie:
 - As a hosted-solution user: Artipie has hosted version at
 https://central.artipie.com - you can sign-up, create repositories
 and deploy artifacts there
 - Create self-hosted installation - you can run Artipie Docker image
 in private network, install k8s [Helm chart](https://github.com/artipie/helm-charts)
 with Artipie or simply run Artipie `jar` file with JVM
 - As a developer - you can use Artipie components to work with artifact repositories
 from code or even build your own artifact manager. Using these component you can
 parse different metadata types, update index files, etc. All components are
 available as java libraries in Maven central: https://github.com/artipie

When using self-hosted Artipie deployment, you have to understand the
[Configuration](https://github.com/artipie/artipie/wiki/Configuration) -
it's stored in storage as yaml files: for single-insance deployment it's
usually a file-system files, for cluster it could be placed in etcd storage
or S3-compatible storage.

To start Artipie Docker check [Quickstart](https://github.com/artipie/artipie#quickstart).

## How to start Artipie service with a maven-proxy repository

In this section we will start Artipie service with a `maven-proxy` repository using JVM. 
Executable `jar` file can be found on the [releases page](https://github.com/artipie/artipie/releases). 
Before running the `jar`, it's necessary to create main Artipie config `yaml` file and 
repository config file. The simplest main Artipie config file `my-artipie.yaml`
has the following content:

```yaml
meta:
  storage:
    type: fs
    path: /var/artipie/repo
```

- field `type` describes which type of [storage](https://github.com/artipie/artipie/wiki/Configuration-Storage#storage) 
Artipie will use to get configuration of repositories, in our example it's `fs` - the file system storage.
- field `path` points to the directory in a file system where repositories config files will be stored.

To get full description how to configure Artipie, please,
check [Configuration](https://github.com/artipie/artipie/wiki/Configuration) page.

It's time to add a `maven-proxy` repository config file, call it `my-maven.yaml`:

```yaml
repo:
 type: maven-proxy
 remotes:
  - url: https://repo.maven.apache.org/maven2
    cache:
     storage:
      type: fs
      path: /var/artipie/data
```
- field `type` describes repository type, in our case it's `maven-proxy`.
- field `url` points to a remote maven repository.
- field `cache` describes storage to keep artifacts gotten from the remote maven repository.

Detailed description for every supported repository type can be found [here](https://github.com/artipie/artipie/tree/master/examples).

As long as we defined `/var/artipie/repo` as path for configuration file system storage,
the file `my-maven.yaml` has to be placed on the path `/var/artipie/repo/my-maven.yaml`
then Artipie service will find it while startup and create repository with name `my-maven`.

Now, you can execute:

```bash
java -jar ./artipie-latest-jar-with-dependencies.jar --config-file=/{path-to-config}/my-artipie.yaml --port=8085
```

- `--config-file` required parameter points to the Artipie main configuration file.
- `--port` optional parameter defines port to start the service.
If `--port` parameter is omitted, Artipie will use `80` as default port.

You should see the following in the console:

```
[main] INFO com.artipie.VertxMain - Artipie was started on port 8085
[ForkJoinPool.commonPool-worker-1] INFO com.artipie.asto.fs.FileStorage - Found 1 objects by the prefix "" in /var/artipie/repo by /var/artipie/repo: [my-maven.yaml]
```

If this is in the console, then everything is OK.
Now, you have your own maven-proxy repository!

You can use this repository as regular maven repository, for example, 
point it in pom file of your java project:

```xml
<repositories>
    <repository>
        <id>artipie</id>
        <url>http://{host}:8085/my-maven/</url>
    </repository>
</repositories>
```

Also, you can define our maven-proxy repository in the maven's settings.xml to use it for any project.

All artifacts obtained through this repository will be stored in the directory `/var/artipie/data/my-maven`
using structure of folders as it does local maven.

To add a new repository or update an existing repository, you have to simply create or modify repositories 
configuration `yaml` files in the directory `/var/artipie/repo`.