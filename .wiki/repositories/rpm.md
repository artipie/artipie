## RPM

Rpm repository is a linux binary packages repository, which [`yum`](https://en.wikipedia.org/wiki/Yum_%28software%29)
and [`dnf`](https://en.wikipedia.org/wiki/DNF_%28software%29) can understand. Try the following
configuration to add rpm repository:

```yaml
repo:
  type: rpm
  storage:
    type: fs
    path: /var/artipie/centos
  settings:
    digest: sha256 # packages digest algorithm
    naming-policy: sha1 # naming policy for metadata files
    filelists: true # is filelist metadata file required
    # repository update mode:
    update:
      # update metadata on package upload
      on: upload
      # or schedule the update
      on:
       cron: 0 2 * * *
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```
Section `setting` allows to configure repository-specific parameters and is not required:
- `digest` - digest algorithm for rpm packages checksum calculation, sha256 (default) and sha1 are supported
- `naming-policy` - naming policy for metadata files: plain, sha1 or sha256 (default) prefixed
- `filelists` - Calculate metadata `filelists.xml`, true by default
- `update` section allows to set update mode: either update the repository when the package is uploaded via HTTP
  or schedule the update via cron

[Permissions configuration](../Configuration-Repository-Permissions.md) section specifies users who can upload and download from the repository.

In order to use Artipie `rpm` repository with `yum` follow the steps:

- Install `yum-utils` if needed: `yum install yum-utils`
- Add Artipie repository: `yum-config-manager --add-repo=http://{host}:{port}/{repository-name}` where `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
  is the name of `rpm` repository
- Refresh the local repository: `yum upgrade all`
- Install the packages: `yum install package-name`

No `yum` nether `dnf` support packages upload, but you can upload `rpm` file into Artipie `rpm`
repository with HTTP `PUT` request:
```commandline
curl -X PUT --data-binary "@my-pkg.rpm" http://{host}:{port}/{repository-name}/my-pkg.rpm?override=true&skip_update=true
```

The request supports the following parameters:
- `override` allows to override existing `rpm` file in the repository, not required, false by default
- `skip_update` can be used to skip repository metadata update, not required, false by default.
  In update mode `cron` this parameter is ignored (as repository metadata are updated by schedule).