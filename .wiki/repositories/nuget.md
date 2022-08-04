## NuGet

[NuGet](https://www.nuget.org/packages) repository is a hosting service for .NET packages, here is 
Artipie repository settings file example:
```yaml
repo:
  type: nuget
  url: http://{host}:{port}/{repository-name}
  storage:
    type: fs
    path: /var/artipie/data/
```

`url` field is required on account of repository specifics, `{host}` and `{port}` are Artipie 
service host and port, `{repository-name}` is the name of the repository. 

To install and publish NuGet packages with `nuget` client into Artipie NuGet repository use 
the following commands:

```bash
# to install the package
$ nuget install MyLib -Version 1.0.0 -Source=http://{host}:{port}/{repository-name}/index.json

# to publish the package
$ nuget push my.lib.1.0.0.nupkg -Source=http://{host}:{port}/{repository-name}/index.json
```