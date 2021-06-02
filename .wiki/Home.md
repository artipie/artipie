# Artipie

To get started with Artipie you need to understand your needs first.
Artipie is not just a binary artifact web server -- it's artifact management
constructor which consists of many components built into server assembly.

You have three options for working with Artipie:
 - As a hosted-solution user: Artipie has hosted version at
 https://central.artipie.com - you can sign-up, create repositories
 and deploy artifacts there
 - Create self-hosted installation - you can run Artipie Docker image
 in private network or install k8s [Helm chart](https://github.com/artipie/helm-charts)
 with Artipie
 - As a developer - you can use Artipie components to work with artifact repositories
 from code or even build your own artifact manager. Using these component you can
 parse different metadata types, update index files, etc. All components are
 available as java libraries in Maven central: https://github.com/artipie

When using self-hosted Artipie deplyment, you have to understand the
[Configuration](https://github.com/artipie/artipie/wiki/Configuration) -
it's stored in storage as yaml files: for single-insance deployment it's
usually a file-system files, for cluster it could be placed in etcd storage
or S3-compatible storage.
