## Maven proxy

Maven proxy repository will redirect all the requests to the remotes. Repository configuration allows
to specify several remotes, Artipie will try to obtain the artifact from the remotes list one by one
while the artifact is not found. If storage is configured, previously downloaded packages will be
available when source repository is down:

```yaml
repo:
  type: maven-proxy
  storage:
    type: fs
    path: /tmp/artipie/maven-central-cache
  # optional; if not defined, then will be used settings form `meta` config
  http_client:
    # all fields are optional
    connection_timeout: 25000
    idle_timeout: 500
    trust_all: true
    follow_redirects: true
    http3: true
    jks:
      path: /var/artipie/keystore.jks
      password: secret
    proxies:
      - url: http://proxy1.com
      - url: https://proxy2.com
        # the HTTP "Basic" authentication defined in RFC 2617
        realm: user_realm
        username: user_name
        password: user_password
  remotes:
    - url: https://repo.maven.apache.org/maven2
      username: Aladdin # optional
      password: OpenSesame # optional
    - url: https://maven.example.com/
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
where `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of maven repository.