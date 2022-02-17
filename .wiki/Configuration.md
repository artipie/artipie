The main Artipie configuration file is `/etc/artipie/artipie.yml`.
It contains server meta configuration, such as:
 - `layout` - `flat` or `org` string
 - `storage` - repositories definition storage config
 - `credentials` - user credentials config

Example: 
```yaml
# /etc/artipie/artipie.yml
meta:
  layout: org
  storage:
    type: fs
    path: /tmp/artipie/data/my-docker
  credentials:
    type: file
    path: _credentials.yml
```

Layout specifies the URI path layout for Artipie. In case of `flat`,
Artipie provides repositories at first path level, e.g. `artipie.host/repo1`,
`artipie.host/repo2`. Only Artipie administrator can manage repositories in flat layout, users can't create new repositories or delete by self.
`org` layout provides hierarchy support for repositories:
URI path has two parts for `org` layouts: `<org>/<repo>`,
where `<org>` is oganisation name, and `<repo>` the name of repository,
e.g. `artipie.host/artipie/maven` - `maven` repository of `artipie` organisation.
In `org` layout, organisation may have a maintainer who can manage
repositories and permissions within organisation; the maintainer can add,
delete and edit repositories, add granular permissions for users for each repository.

Storage - is a [storage configuration](https://github.com/artipie/artipie/wiki/Configuration-Storage)
for [repository definitions](https://github.com/artipie/artipie/wiki/Configuration-Repository).
It locates a storage where all config files for each repository are located. Keep in mind,
Artipie user should have read and write permissions for this storage.

Credentials - provider for user credentials,
it can be managed only by Artipie server administrator.
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
process until success with one of them. 
For example, the config may look like the following:
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
If `github` is not successful, user authentication is failed.
