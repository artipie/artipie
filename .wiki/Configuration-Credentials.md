## Credentials and policy

Credentials section in main Artipie configuration allows to set four various credentials sources:
`yaml` files with users' info, GitHub accounts, user from environment and Keycloak authorization. 
Here is the example of the full `credentials` section: 

```yaml
meta:
  credentials:
    - type: artipie
      storage: 
        type: fs
        path: /tmp/artipie/security
    - type: github
    - type: env
    - type: keycloak
      url: http://localhost:8080
      realm: realm_name
      client-id: client_application_id
      client-password: client_application_password
  policy:
    type: artipie
    storage:
      type: fs
      path: /tmp/artipie/security
```
Each item of `credentials` list has only one required field - `type`, which determines the type of
authentication:
- `artipie` stands for auth by credentials from YAML files from specified storage. Storage configuration
is required only if `artipie` policy is not set
- `github` is for auth via GitHub
- `env` authenticates by credentials from environment
- `keycloak` is for auth via Keycloak

When several credentials types are set, Artipie tries to authorize user via each method.

Policy section is responsible for access permissions and the only supported type out of the box 
for now is `artipie`. If policy section is absent, access to any repository is allowed for any 
authenticated user.

### Credentials type `artipie`

If the `type` is set to `artipie`, configured credentials storage is expected to have the following structure:
```
├── users
│   ├── david.yaml
│   ├── jane.yaml
│   ├── Alice.yml
│   ├── ...
```
where the name of the file is the name of the user (case-sensitive), both `yml` and `yaml` extensions are
supported. File content should have the following structure:
```yaml
type: plain # plain and sha256 types are supported
pass: qwerty
email: david@example.com # Optional
enabled: true # optional default true
```
where `type` is password format: `plain` and `sha256` types are supported. Required fields for each 
user are `type` and `pass`. If `type` is `sha256`, then SHA-256 checksum of the password is expected 
in the `pass` field.

`email` field is optional, the email is not actually used anywhere for now.

`enabled` field is optional, if set to `false` user is considered as deactivated and is not authenticated.

User info file can also describe user roles and permissions, check [policy documentation](./Configuration-Policy) for more details.

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
    - type: keycloak
      url: http://localhost:8080
      realm: demorealm
      client-id: demoapp
      client-password: secret
```

To interact with Keycloak server the Artipie uses `Direct Access Grants` authentication flow 
and directly requests user login and password. 
Artipie acts as Keycloak client application and should be configured in Keycloak with following settings:
* Client authentication: On
* Authorization: On
* Authentication flow: `Direct access grants`

## Custom authentication

Artipie allows implementing and using custom authentication (credentials type). To be more precise,
you can choose any way to authenticate users you like, implement it in Java and use in Artipie. To do
so:

- Add [`http` module](https://github.com/artipie/http) to your project dependencies.
- Implement [Authentication](https://github.com/artipie/http/blob/master/src/main/java/com/artipie/http/auth/Authentication.java) interface 
to verify and obtain users the way you need. Note, that authenticated users are cached on the upper level,
so, there is no need to worry about caching in your implementation.
- Implement [factory](https://github.com/artipie/http/blob/master/src/main/java/com/artipie/http/auth/AuthFactory.java) for new authentication
and add [factory annotation](https://github.com/artipie/http/blob/master/src/main/java/com/artipie/http/auth/ArtipieAuthFactory.java) 
with authentication type name. As an example, check [KeycloakFactory](https://github.com/artipie/artipie/blob/master/src/main/java/com/artipie/auth/AuthFromKeycloakFactory.java).
- Add your package to Artipie class-path and to `AUTH_FACTORY_SCAN_PACKAGES` environment variable
- Specify your authentication type in the credentials list. Note, that Artipie tries to authenticate 
user via each listed type. So, if you are not actually using any auth type it would be better 
to remove it from the settings.

For more details and examples, check the following packages: [com.artipie.http.auth](https://github.com/artipie/http/tree/master/src/main/java/com/artipie/http/auth) 
and [com.artipie.auth](https://github.com/artipie/artipie/tree/master/src/main/java/com/artipie/auth).