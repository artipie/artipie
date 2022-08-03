## File

Files repository is a general purpose file storage which provides API for upload and download: `PUT` requests for upload and `GET` for download.
To set up this repository, create config with `file` repository type and storage configuration. [Permissions configuration](../Configuration-Repository Permissions.md)
can authorize users allowed to upload and download.

*Example:*
```yaml
repo:
  type: file
  storage: default
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```

In order to upload a binary file to the storage, send a `PUT` HTTP request with file contents:

```bash
echo "hello world" > test.txt
curl -X PUT --data-binary "@test.txt" http://{host}:{port}/{repository-name}/test.txt
```

In order to download a file, send a `GET` HTTP request:

```bash
curl -X GET http://{host}:{port}/{repository-name}/text.txt
```

In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of files repository.