## Maven

To host a [Maven](https://maven.apache.org/) repository for Java artifacts and dependencies try the
following configuration:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /tmp/artipie/data
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```

To use this repository as regular maven repository in Java project, add the following configuration
into `pom` project file (alternatively [configure](https://maven.apache.org/guides/mini/guide-multiple-repositories.html)
it via [`~/.m2/settings.xml`](https://maven.apache.org/settings.html)):

```xml
<repositories>
    <repository>
        <id>{artipie-server-id}</id>
        <url>http://{host}:{port}/{repository-name}</url>
    </repository>
</repositories>
```
Then run `mvn install` (or `mvn install -U` to force download dependencies).

To deploy the project into Artipie repository, add [`<distributionManagement>`](https://maven.apache.org/pom.html#Distribution_Management)
section to [`pom.xml`](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
project file (don't forget to specify authentication credentials in
[`~/.m2/settings.xml`](https://maven.apache.org/settings.html#Servers)
for `artipie` server):

```xml
<project>
  [...]
  <distributionManagement>
    <snapshotRepository>
      <id>artipie</id>
      <url>http://{host}:{port}/{repository-name}</url>
    </snapshotRepository>
    <repository>
      <id>artipie</id>
      <url>http://{host}:{port}/{repository-name}</url>
    </repository>
  </distributionManagement>
</project>
```
In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of maven repository.