## Debian

Debian repository is [Debian](https://www.debian.org/index.en.html) and [Ubuntu](https://ubuntu.com/) 
linux packages repository, that [`apt-get`](https://en.wikipedia.org/wiki/APT_(software)) can understand. 
To create Debian repository in Artipie, try the following configuration:
```yaml
repo:
  type: deb
  storage:
    type: fs
    path: /var/artipie/my-debian
  settings:
    Components: main
    Architectures: amd64
    gpg_password: 1q2w3e4r5t6y7u
    gpg_secret_key: secret-keys.gpg
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```
where 
- `Components` is a space separated list of the repository components
(in other words [components can be called areas or subdirectories](https://wiki.debian.org/DebianRepository/Format#Components)), required;
- `Architectures` is a space separated [list of the architectures](https://wiki.debian.org/DebianRepository/Format#Architectures),
supported by the repository, required;
- Debian repository supports gpg signature, to enable it, provide gpg password in `gpg_password` field and 
secret file location `gpg_secret_key` relatively to [Artipie configuration storage](../Configuration.md).

Check [storage](../Configuration-Storage.md) and [permission](../Configuration-Repository%20Permissions.md)
documentations to learn more about these settings.

To use Artipie Debian repository, add local repository to the list of repos for `apt` by adding
the following line to the `/etc/apt/sources.list`:

```text
deb [trusted=yes] http://{username}:{password}@{host}:{port}/{repository-name} {repository-name} {components}
```
where `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file),
{components} in the list of components you specified in repo config file. Username and password are
credentials of Artipie user. If gpg signing is enabled, parameter `[trusted=yes]` can be skipped.

Now use `apt-get` to install the package:
```commandline
apt-get update
apt-get install -y my-package
```

`apt-get` client does not support upload command, but it's possible to add package into Artipie 
Debian repository with simple HTTP `PUT` request:
```bash
curl http://{username}:{password}@{host}:{port}/{repository-name}/{component} --upload-file /path/to/package.deb
```
where {component} is one of the `Components` list value.
Once the package is uploaded, Artipie will update repository indexes.