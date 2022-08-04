## Credentials

Credentials section in main Artipie configuration allows to set three various credentials sources:
`yaml` file with users list, GitHub accounts and user from environment. Here is the example of the
full `credentials` section: 

```yaml
meta:
  credentials:
    -
      type: file
      path: _credentials.yml
    -
      type: github
    -
      type: env
```
Each item of `credentials` list has only one required field - `type`, which determines the type of
authentication:
- `file` stands for auth by credentials from another YAML file (path to the file is specified by 
`path` field value, it's relative to main configuration storage)
- `github` is for auth via github
- `env` authenticates by credentials from environment

When several credentials types are set, Artipie tries to authorize user via each method.

### Credentials type `file`

If the `type` is set to `file`, another YAML file is required in the storage, with
a list of users who can be authorized by Artipie service:

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

where `type` is password format: `plain` and `sha256` types are supported. Required fields for each 
user are `type` and `pass`. If `type` is `sha256`, then SHA-256 checksum of the password is expected 
in the `pass` field.

`email` field is optional, the email is not actually used anywhere for now.

Users can be assigned to some groups, all repository permissions granted to the group are applied
to the users participating in this group. More information about repository permissions can be found
[here](./Configuration-Repository-Permissions.md).

### Credentials type `github`

If the `type` is set to `github`, GitHub username with `github.com/` prefix `github.com/{username}` 
and [personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) 
can be used to login into Artipie service. GitHub token can be obtained in the section 
"Developer settings" of personal settings page.

### Credentials type `env`

If the `type` is set to `env`, the following environment variables are expected:
`ARTIPIE_USER_NAME` and `ARTIPIE_USER_PASS`. For example, you start
Docker container with the `-e` option:

```bash
docker run -d -v /var/artipie:/var/artipie` -p 80:80 \
  -e ARTIPIE_USER_NAME=artipie -e ARTIPIE_USER_PASS=qwerty \
  artipie/artipie:latest
```

Authentication from environment allows adding only one user and is meant for tests or try-it-out 
purposes only.
