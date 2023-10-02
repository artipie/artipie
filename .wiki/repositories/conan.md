## Conan

Conan is [package manager for C++](https://conan.io/), which supports different operating systems and compilers.
Currently, Conan client 1.x is supported. Protocol version 'v1' (default) is recommended.

### Adapter configuration

```yaml
repo:
  type: conan
  url: http://artipie:9300/my-conan
  port: 9300
  storage:
    type: fs
    path: /var/artipie/data/
```

### Client configuration

Conan client supports using multiple remote repository servers for downloading and uploading.
Package could be downloaded from one server and uploaded to the other. After installation conan client has Conan Central repository added.
User can add and use custom server by its URL with the command below. Note that `False` is required to force disable SSL protocol.
When you upload or download you can specify remote repository explicitly by `-r` option.

```bash
conan remote add conan-test http://artipie.artipie:9300 False
# Usage:
conan upload -r conan-test ....
conan download -r conan-test ...
```
