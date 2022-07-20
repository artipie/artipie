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

## How to start Artipie service

To start Artipie Docker check [Quickstart](https://github.com/artipie/artipie#quickstart) section, here we focus on starting Artipie `jar` file with JMV. Executable file with dependencies can be found on each github release page. Before running the `jar`, it's necessary to create main Artipie config yaml (check [Configuration](https://github.com/artipie/artipie/wiki/Configuration) page for full description), the example configuration can be found in [resources example folder](https://github.com/artipie/artipie/tree/master/src/main/resources/example). Copy the folder into any convenient place on your file system and correct storage path in `artipie.yaml` to point to the `repo` subdirectory on your file system. Now, you can execute:

```
java -jar ./artipie-latest-jar-with-dependencies.jar --config-file=/path-to-config/artipie.yaml --port=8081
```

Required parameter `--config-file` points into to the Artipie main configuration file, `--port` is optional and configures port to start the service. 

Example configuration folder contains several yaml files: 
- `_credentials.yaml` - this is the file with existing users and user's info, detailed description can be found [here](https://github.com/artipie/artipie#multitenancy)
- `_storages.yaml` - storage aliases configuration, check [this](https://github.com/artipie/artipie/wiki/Configuration-Storage#aliases) page
- several yamls files with different storages' configuration. Detailed description for every supported repository type can be found [here](https://github.com/artipie/artipie/tree/master/examples). 

To add or update any repository, you can simply modify or create repositories configuration yamls.