### Binary Repo

[![](https://github.com/artipie/artipie/workflows/Proof::binary/badge.svg)](./examples/binary)

This directory contains an example of how to can use artipie as a storage for binary files.
Try this example by running `run.sh` script.

Basic configuration `repo.yaml`:

```yaml
repo:
  type: file
  storage:
    type: fs
    path: /var/artipie/data
```

After creating the configuration file below, artipie is ready to server as a storage for binary files.

In order to upload a binary file to artipie, send a PUT HTTP request with file contents:

```bash
echo "hello world" > text.txt
curl --silent -X PUT --data-binary "@text.txt" http://localhost:8080/repo/text.txt
```

In order to download a file, send a GET HTTP request:

```bash
curl -X GET http://localhost:8080/repo/text.txt
```

#### Advanced option

The binary type of repositories does not have any other opinions available.
