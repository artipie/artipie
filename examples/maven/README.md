### Maven Repo

![](https://github.com/artipie/artipie/workflows/Proof::maven/badge.svg)

Try this `maven.yaml` file to host a [Maven](https://maven.apache.org/) repo:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /var/artipie/maven
```

Add [`<distributionManagement>`](https://maven.apache.org/pom.html#Distribution_Management)
section to your
[`pom.xml`](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
(don't forget to specify authentication credentials in
[`~/.m2/settings.xml`](https://maven.apache.org/settings.html#Servers)
for `artipie` server):

```xml
<project>
  [...]
  <distributionManagement>
    <snapshotRepository>
      <id>artipie</id>
      <url>http://localhost:8080/maven</url>
    </snapshotRepository>
    <repository>
      <id>artipie</id>
      <url>http://localhost:8080/maven</url>
    </repository>
  </distributionManagement>
</project>
```

Then, `mvn deploy` your project.

Add [`<repository>`](https://maven.apache.org/pom.html#Repositories) and
[`<pluginRepository>`](https://maven.apache.org/pom.html#Repositories)
to your `pom.xml` (alternatively
[configure](https://maven.apache.org/guides/mini/guide-multiple-repositories.html)
it via
[`~/.m2/settings.xml`](https://maven.apache.org/settings.html)) to use deployed artifacts:

```xml
<project>
  [...]
  <pluginRepositories>
    <pluginRepository>
      <id>artipie</id>
      <name>artipie plugins</name>
      <url>http://localhost:8080/maven</url>
    </pluginRepository>
  </pluginRepositories>
  <repositories>
    <repository>
      <id>artipie</id>
      <name>artipie builds</name>
      <url>http://localhost:8080/maven</url>
    </repository>
  </repositories>
</project>
```

Run `mvn install` (or `mvn install -U` to force download dependencies).

### Maven proxy Repo

Try this `maven-proxy.yaml` file to host a proxy to Maven central:

```yaml
repo:
  type: maven-proxy
  remotes:
    - url: https://repo.maven.apache.org/maven2
      username: Aladdin # optional
      password: OpenSesame # optional
```

Artipie will redirect all requests to Maven central.

Proxy repository supports caching in local storage.
To enable it and make previously accessed images available when source repository is down 
add `storage` section to config:

```yaml
repo:
  type: maven-proxy
  remotes:
    - url: https://repo.maven.apache.org/maven2
      storage:
        type: fs
        path: /tmp/artipie/data/my-maven-cache
```