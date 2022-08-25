# Repository permissions

Permissions for repository operations can be granted in the [repository configuration file](Configuration-Repository.md):
```yaml
repo:
  ...
  permissions:
    "*": # any user can download
      - download
    jane: # jane can also write
      - write
    mark: # any operation is allowed for mark
      - "*"
```

All repositories support `read` and `write` operations, other specific permissions may be supported
in certain repository types. `read` and `write` are basic operations for each repository type, 
some repositories also support `delete`. Operations can be set in `permissions` section with the 
following synonyms:
- `read`,`r`, `download`, `install`
- `write`, `w`, `publish`, `push`, `deploy`, `upload`
- `delete`, `d`

Operations names are case-sensitive. Also, there is a wildcard `*` which means  
"any user" if placed on username level of yaml mapping or  
"any operation" if placed on operations yaml sequence level.

To grant permissions to the user's group, add this group to `permissions` section starting with slash:
```yaml
repo:
  ...
  permissions:
    /readers: # group "readers" can download
      - read
    /admin: # any operation is allowed for "admin" group
      - "*"
```
Operations and their synonyms for users and groups are the same, users and groups can be mixed in 
`permissions` section, the only rule here - groups names should start with `/` sign. Permissions granted 
to the group are applied to the users participating in this group. Check [Credentials](./Configuration-Credentials.md) 
section to find out how to add users group.

If `permissions` section is absent in repo config, then any supported operation is allowed for everyone,
empty `permissions` section restricts any operations for anyone. 

## Docker repository granular permissions

Permissions for docker repository can be configured as described above, but besides docker-adapter 
supports granular permissions, which means that some operations can be granted for specific scope 
and image. Here is the example docker repository settings yaml:
```yaml
repo:
  type: docker
  storage:
    type: fs
    path: /var/artipie/data
  permissions:
    alice:
      - repository:my-alpine:*
      - repository:ubuntu-latest:pull
      - repository:some-image:overwrite
      - repository:some-image:push
    mike:
      - repository:some-image:push
      - repository:some-image:pull
    bob:
      - registry:base:*
      - registry:catalog:*
```
Each permission consists of three parts: `scope:name:action`, `scope` can be either "registry" for
the registry level operations or "repository". `Name` is the access resource name, `action` can be
one of `pull`, `push`, `overwrite` or `*` (`*` stands for any action), `overwrite` works
for overwriting existing tags and allows creating new tags by default. 