### NuGet Repo

![](https://github.com/artipie/artipie/workflows/Proof::nuget/badge.svg)

Try this `nuget.yaml` file:

```yaml
repo:
  type: nuget
  path: my-nuget
  url: http://localhost:8080/my-nuget
  storage:
    type: fs
    path: /tmp/artipie/data/my-nuget
```

To publish your NuGet package use the following command:

```bash
$ nuget push my.lib.1.0.0.nupkg -Source=http://localhost:8080/my-nuget/index.json
```

To install the package into a project use the following command:

```bash
$ nuget install MyLib -Version 1.0.0 -Source=http://localhost:8080/my-nuget/index.json
```