## Credentials

Credentials section in main Artipie configuration allows to set four various credentials sources:
`yaml` file with users list, GitHub accounts, user from environment and Keycloak authorization. Here is the example of the
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
    -
      type: keycloak
      url: http://localhost:8080
      realm: realm_name
      client-id: client_application_id
      client-password: client_application_password
```
Each item of `credentials` list has only one required field - `type`, which determines the type of
authentication:
- `file` stands for auth by credentials from another YAML file (path to the file is specified by 
`path` field value, it's relative to main configuration storage)
- `github` is for auth via GitHub
- `env` authenticates by credentials from environment
- `keycloak` is for auth via Keycloak

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
[here](./Configuration-Repository-Permissions).

### Credentials type `github`

If the `type` is set to `github`, GitHub username with `github.com/` prefix `github.com/{username}` 
and [personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) 
can be used to log in into Artipie service. GitHub token can be obtained in the section 
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

### Credentials type `keycloak`

If the 'type' is set to `keycloak`, the following Yaml attributes are required:
* `url` Keycloak authentication server url.
* `realm` Keycloak realm.
* `client-id` Keycloak client application id.
* `client-password` Keycloak client application password.

Example:
```yaml
meta:
  credentials:
    - 
      type: keycloak
      url: http://localhost:8080
      realm: demorealm
      client-id: demoapp
      client-password: secret
```

To interact with Keycloak server the Artipie uses `Direct Access Grants` authentication flow 
and directly requests user login and password. 
Artipie acts as Keycloak client application and should be configured in Keycloak with following settings :
* Client authentication: On
* Authorization: On
* Authentication flow: `Direct access grants`