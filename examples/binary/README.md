### Binary repository

[![](https://github.com/artipie/artipie/workflows/Proof::binary/badge.svg)](./examples/binary)

Try this `repo.yaml` file:

<pre>
repo:
  type: file
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    "*":
      - "*"
</pre>

Use `PUT` HTTP request to upload a file, and `GET` for downloading.</p>

Example using [HTTPie](https://httpie.org/) CLI tool for uploading and downloading:
<pre>
http -a {{user}}:password PUT https://central.artipie.com/{{user}}/{{name}}/file.bin @file.bin
http GET https://central.artipie.com/{{user}}/{{name}}/file.bin --output=./file.bin
</pre>

// TODO: add curl example
