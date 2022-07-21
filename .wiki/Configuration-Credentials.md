## Credentials

```yaml
meta:
  credentials:
    type: file
    path: _credentials.yml
```

If the `type` is set to `file`, another YAML file is required in the storage, with
a list of users who will be allowed to create repos
(`type` is password format, `plain` and `sha256` types are supported):

```yaml
credentials:
  jane:
    type: plain
    pass: qwerty
    email: jane@example.com # Optional
  john:
    type: sha256
    pass: xxxxxxxxxxxxxxxxxxxxxxx
    groups: # Optional
      - readers
      - dev-leads
```
Users can be assigned to some groups, all repository permissions granted to the group are applied
to the users participating in this group.

If the `type` is set to `env`, the following environment variables are expected:
`ARTIPIE_USER_NAME` and `ARTIPIE_USER_PASS`. For example, you start
Docker container with the `-e` option:

```bash
docker run -d -v /var/artipie:/var/artipie` -p 80:80 \
  -e ARTIPIE_USER_NAME=artipie -e ARTIPIE_USER_PASS=qwerty \
  artipie/artipie:latest
```

The type of credentials config could be `file` (support other types such
as database or LDAP will be added later). For file credentials, it has a `path`
parameter - it's a relative key for repository storage configuration.
It contains passwords validators for username keys in this format:
```yaml
credentials:
  user1:
    pass: "sha256:abc...123"
  user2:
    pass: "plain:secret-password"
    groups:
      - readers
```
Credentials file has a `credentials` root element with map of users, each user has a `pass` text element which specify a password validator in this format:
`<type>:<data>`, where type is either `sha256` or `plain`
(notice: it's not recommended to use plain password validation,
it's added for debugging purpose), `sha256` validator data includes
a SHA256 hash-sum of user's password, `plain` validator has a plain
password string data.
Also, users can be assigned to groups, all permissions granted to the group
in repository are applied to the users participating in this group.

If the `type` is set to `env`, the following environment variables are expected:
`ARTIPIE_USER_NAME` and `ARTIPIE_USER_PASS`. For example, you start
Docker container with the `-e` option:
```bash
docker run -d -v /var/artipie:/var/artipie` -p 80:80 \
  -e ARTIPIE_USER_NAME=artipie -e ARTIPIE_USER_PASS=qwerty \
  artipie/artipie:latest
```
There is an ability to use GitHub credentials to authenticate users.
You should specify `type` as `github` to do this.
You can specify several types of credentials; in that case, authentication will
process until one of them is successful.
For example, the config may look like following:
```yaml
meta:
  layout: org
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
  credentials:
    - type: env
    - type: github
```
If `env` is not successful, `github` will be applied to authenticate a user.
If `github` is not successful, user authentication will fail.