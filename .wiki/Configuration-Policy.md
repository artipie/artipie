# Policy

Artipie provides out of the box policy type `artipie` and possibility to implement and use custom
policy. Generally in artipie, policy is the format in which permissions and roles are granted and 
assigned to users. 

## Artipie policy 

Artipie policy format is the set of yaml files, where permissions for users and roles are described.
The policy type is configured in the main configuration file:
```yaml
meta:
  credentials:
    - type: artipie
    - type: github
    - type: env
    - type: keycloak
      url: http://localhost:8080
      realm: realm_name
      client-id: client_application_id
      client-password: client_application_password
  policy:
    type: artipie
    eviction_millis: 180000 # optional, default 3 min
    storage: # required
      type: fs
      path: /tmp/artipie/sequrity
```
Under the hood, artipie policy uses [guava cache](https://github.com/google/guava/wiki/CachesExplained), 
eviction time can be configured with the help of `eviction_millis` field.  
Policy storage is supposed to have the following format:
```
├── roles
│   ├── java-dev.yaml
│   ├── admin.yaml
│   ├── testers.yml
│   ├── ...
├── users
│   ├── david.yaml
│   ├── Alice.yml
│   ├── jane.yaml
│   ├── ...
```
where the name of the file is the name of the user or role (case-sensitive), both `yml` and `yaml` 
extensions are supported.  
User file content should have the following structure:
```yaml
# user auth info for credentials type `artipie`
type: plain # plain and sha256 types are supported
pass: qwerty

# policy info
enabled: true # optional default true
roles:
  - java-dev
  - testers
permissions:
  artipie_basic_permission:
    rpm-repo:
      - read
```
Note, that `type` and `pass` fields are required only if user is authenticated via
`artipie` authentication. If, for example, user is authenticated via github, only policy-related fields
`roles` and `permissions` should be present in the user info files.

Both `roles` and `permissions` fields are optional, if none are present or `enabled` is set to `false`
user does not have any permissions for any repository.

Role file content should have the following structure:
```yaml
# java-dev.yaml
enabled: true # optional default true
permissions:
  adapter_basic_permission:
    maven-repo:
      - read
      - write
    python-repo:
      - read
    npm-repo:
      - read
```
Role can also be deactivated (it means that role does not grant any permissions for the user) if
`enabled` is set to `false`.

Individual user permissions and role permissions are simply joined for the user.

### Permissions

Permissions in Artipie are based on `java.security.Permission` and `java.security.PermissionCollection`
and support all the principals of java permissions model. There is no way to for explicitly forbid
some action for user or role, for each user permissions are combined from user individual permissions
and role permissions.  
Permissions for users and roles are set in the same format.

#### Adapter basic permission

```yaml
permissions:
  adapter_basic_permission:
    npm-repo:
      - "*" # any action is allowed
    maven-repo:
      - install
      - deploy
    john/python-repo:
      - read
```
`adapter_basic_permission` is the [permission type name](https://github.com/artipie/http/blob/master/src/main/java/com/artipie/security/perms/AdapterBasicPermissionFactory.java).
This type is the permission type for any repository except for docker. Permission
config of the `adapter_basic_permission` is the set of repository names with action list. 
The following actions and synonyms are supported:
- read, r, download, install, pull
- write, w, publish, push, deploy, upload
- delete, d, remove 

Action `delete` in not supported by each adapter, check specific adapter docs for more details. 

Wildcard `*` is supported as for actions (check the example above) as for repository name:
```yaml
permissions:
  adapter_basic_permission:
    "*":
      - read
```
which means that `read` actions is allowed for any repository.

Note, that in the case of `org` layout repository name is combined from username and repository name and
should be specified correspondingly: `jane/pypi-public` or `mark/maven-repo`.

#### Adapter all permission

Artipie also support `adapter_all_permission` type to allow [any actions for any repository](https://github.com/artipie/http/blob/master/src/main/java/com/artipie/security/perms/AdapterAllPermissionFactory.java):

```yaml
permissions:
  adapter_all_permission: {}
```
Grant such permissions carefully.

## Custom policy

Artipie allows implementing and using custom policy. To be more precise, you can choose some other 
format to specify user and roles permissions and other storage to keep it (some database for example) 
and tell artipie to use it. To do so: 
- add [`http` module](https://github.com/artipie/http) to your project dependencies 
- create `Policy` implementation to provide user `PermissionCollection`. Note, that this implementation should probably use some
cache as reading permissions on each operation can be very time-consuming
- implement `PolicyFactory` with `ArtipiePolicyFactory` annotation to create the instance of you custom policy
- add your package to artipie class-path and to `POLICY_FACTORY_SCAN_PACKAGES` environment variable
- specify your policy and other necessary parameters in the main configuration

Check [existing code in the security package](https://github.com/artipie/http/tree/master/src/main/java/com/artipie/security) 
of [`http` module](https://github.com/artipie/http) for more details. 