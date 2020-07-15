### Gem Repo

Try this `gem.yaml` file:

```yaml
repo:
  type: gem
  storage:
    type: fs
    path: /tmp/artipie/data/my-nuget
```

Publish a gem:

```bash
$ gem push my_first_gem-0.0.0.gem --host http://localhost:8080/gem
```

Install a gem:

```bash
$ gem install my_first_gem --source http://localhost:8080/gem
```
