### Debian Repo

![](https://github.com/artipie/artipie/workflows/Proof::deb/badge.svg)

Create new directory `/var/artipie`, directory for configuration files
`/var/artipie/repo` and directory for Debian repository `/var/artipie/my-debian`.
Put repository config file to `/var/artipie/repo/my-debian.yaml`:

```yaml
repo:
  type: deb
  storage:
    type: fs
    path: /var/artipie/my-debian
```

Start Artipie Docker image:

```bash
$ docker run -v /var/artipie:/var/artipie artipie/artipie
```

On the client machine add local repository to the list of Debian packages for `apt` by adding 
the following line to the `/etc/apt/sources.list`:

```text
deb [trusted=yes] http://username:password@localhost:8080/my-debian my-debian main
```