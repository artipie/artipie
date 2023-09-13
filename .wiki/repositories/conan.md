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

```bash
conan remote add conan-test http://artipie.artipie:9300 False
```
