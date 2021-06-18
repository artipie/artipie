
## File

Files repository is a general purpose file storage which provides API for upload and download: `PUT` requests for upload and `GET` for download.
To setup this repository create config with `file` repository type and storage configuration. Permissions configuration can authorize
users allowed to upload and download.

*Example:*
```yaml
repo:
  type: file
  storage: default
  permissions:
    alice:
      - upload
      - download
    \*:
      - download
```

## File proxy (mirror)

File proxy or mirror is a general purpose files mirror. It acts like a transparent HTTP proxy for one or multiple hosts
and caches all the data locally. To configure it use `file-proxy` repository type with required `remotes` section which should include at least
one remote configuration. Each remote config must provide `url` for remote file server and optional `username` and `password` for authentication.
Proxy is a read-only repository, nobody can upload to it. Only `download` permissions make sence here. Storage can be configured for
caching capabilities.

*Example:*
```yaml
repo:
  type: file-proxy
  storage: default
  remotes:
    - url: "https://my-remote1.com"
    - url: "https://secondary-remote.com"
      username: "alice"
      password: "qwerty"
  permissions:
    \*:
      - download
```
