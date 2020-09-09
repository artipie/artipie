### Binary Repo

[![](https://github.com/artipie/artipie/workflows/Proof::binary/badge.svg)](./examples/binary)

Try this `repo.yaml` file:

```yaml
repo:
  type: file
  storage:
    type: fs
    path: /var/artipie/data
```

You can send HTTP PUT/GET requests
to `http://localhost:8080/repo/<filename>` to upload/download a binary file,
e.g. `http://localhost:8080/repo/libsqlite3.so`.
