### Maven Repo

![](https://github.com/artipie/artipie/workflows/Proof::maven/badge.svg)

Try this `maven.yaml` file to host a [Maven](https://maven.apache.org/) repo:

```yaml
repo:
  type: maven
  storage:
    type: fs
    path: /var/artipie/maven
  permissions:
    {{user}}:
      - upload
      - download
    "*":
      - download
```

With this configuration,
the user `{{user}}` will be able to publish Maven artifacts,
and all other users will be able to download.

This is how you may configure it inside your
<a href="https://maven.apache.org/guides/introduction/introduction-to-the-pom.html"><code>pom.xml</code></a>:</p>

<pre>&lt;project&gt;
  [...]
  &lt;distributionManagement&gt;
    &lt;snapshotRepository&gt;
      &lt;id&gt;artipie&lt;/id&gt;
      &lt;url&gt;https://central.artipie.com/{{user}}/{{name}}&lt;/url&gt;
    &lt;/snapshotRepository&gt;
    &lt;repository&gt;
      &lt;id&gt;artipie&lt;/id&gt;
      &lt;url&gt;https://central.artipie.com/{{user}}/{{name}}&lt;/url&gt;
    &lt;/repository&gt;
  &lt;/distributionManagement&gt;
  &lt;repositories&gt;
    &lt;repository&gt;
      &lt;id&gt;artipie&lt;/id&gt;
      &lt;url&gt;https://central.artipie.com/{{user}}/{{name}}&lt;/url&gt;
    &lt;/repository&gt;
  &lt;/repositories&gt;
&lt;/project&gt;</pre>

<p>You publish just with
<a href="https://maven.apache.org/plugins/maven-deploy-plugin/usage.html"><code>mvn deploy</code></a>
and you download with
<a href="https://maven.apache.org/plugins/maven-compiler-plugin/index.html"><code>mvn compile</code></a>.