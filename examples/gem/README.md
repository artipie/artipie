### Gem Repo

![](https://github.com/artipie/artipie/workflows/Proof::gem/badge.svg)

This directory contains a basic example of how artipie can be used as a Gem repository. 
Try this example by running `run.sh` script.

Basic configuration `gem.yaml`:

```yaml
repo:
  type: gem
  storage:
    type: fs
    path: /var/artipie/data
```

After creating the configuration file below, the configured Gem repository is ready for use.

Before uploading your gems, you should obtain a key for authorization. A base64 encoded
login:password would be a valid key:

```bash
export GEM_HOST_API_KEY=$(echo -n "hello:world" | base64)
```

In order to upload a `.gem` file into, use `gem push` command:

```bash
$ gem push my_first_gem-0.0.0.gem --host http://localhost:8080/gem
```

In order to install an existing gem , use `gem install` command:

```bash
$ gem install my_first_gem --source http://localhost:8080/gem
```

#### Advanced option

Gem repositories does not have any other opinions.
