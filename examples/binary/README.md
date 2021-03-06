### Binary Repo

[![](https://github.com/artipie/artipie/workflows/Proof::binary/badge.svg)](./examples/binary)

This directory contains a basic example of  how artipie can be used as a storage for binary files. 
Try this example by running `run.sh` script.

Basic configuration `repo.yaml`:

```yaml
repo:
  type: file
  storage:
    type: fs
    path: /var/artipie/data
```

After creating the configuration file below, the configured binary storage is ready for use.

In order to upload a binary file to the storage, send a PUT HTTP request with file contents:

```bash
echo "hello world" > text.txt
curl --silent -X PUT --data-binary "@text.txt" http://localhost:8080/repo/text.txt
```

In order to download a file, send a GET HTTP request:

```bash
curl -X GET http://localhost:8080/repo/text.txt
```

### Proxy Binary Repo

Try this `repo-proxy.yaml` file to host files repository proxy:

```yaml
repo:
  type: file-proxy
  remotes:
    - url: https://example-storage.com/my-files
      username: Aladdin # optional
      password: OpenSesame # optional
```

Artipie will redirect all requests to remote.
