### RPM Repo

Create new directory `/var/artipie`, directory for configuration files
`/var/artipie/repo` and directory for RPM repository `/var/artipie/centos`.
Put repository config file to `/var/artipie/repo/centos.yaml`:

```yaml
repo:
  type: rpm
  storage:
    type: fs
    path: /var/artipie/centos
  settings:
    digest: sha256 # Digest algorithm for rpm packages checksum calculation, sha256 (default) and sha1 are supported
    naming-policy: sha1 # Naming policy for metadata files: plain, sha1 or sha256 (default) prefixed
    filelists: true # Calculate metadata filelists.xml, true by default
      
```

Put all RPM packages to repository directory: `/var/artipie/centos/centos`.

Optional: generate metadata using [CLI tool](https://github.com/artipie/rpm-adapter/).

Start Artipie Docker image:

```bash
$ docker run -v /var/artipie:/var/artipie artipie/artipie
```

On the client machine add local repository to the list of repos:

 - Install `yum-utils` if needed: `yum install yum-utils`
 - Add repository: `yum-config-manager --add-repo=http://yourepo/`
 - Refresh the repo: `yum upgrade all`
 - Download packages: `yum install package-name`
