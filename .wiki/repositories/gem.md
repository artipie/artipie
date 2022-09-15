## Gem

[Gem repository](https://rubygems.org/) supports hosting of Ruby packages. Here is configuration example:
```yaml
repo:
  type: gem
  storage:
    type: fs
    path: /var/artipie/data/
  permissions:
    "*":
      - download
    mark:
      - upload
```
Check
[storage](./Configuration-Storage) and [permission](./Configuration-Repository-Permissions)
documentations to learn more about these settings.

Before uploading gems, obtain a key for authorization and set it to `GEM_HOST_API_KEY` environment variable. 
A base64 encoded `{username}:{password}` pair would be a valid key:
```bash
export GEM_HOST_API_KEY=$(echo -n "{username}:{password}" | base64)
```
In order to upload a `.gem` file into Artipie Gem repository, use `gem push` command and 
`--host` option to specify repository URL:
```bash
$ gem push my_gem-0.0.0.gem --host http://{host}:{port}/{repository-name}
```
In order to install an existing gem package, use `gem install` command and `--source` option to 
specify repository ULR:
```bash
$ gem install my_gem --source http://{host}:{port}/{repository-name}
```

In the example above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file),
`{username}` and `{password}` are Artipie user credentials.