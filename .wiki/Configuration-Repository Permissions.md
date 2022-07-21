## Repository permissions

Permissions for repository operations can be granted in the repo configuration file:
```yaml
repo:
  ...
  permissions:
    jane:
      - read
      - write
    admin:
      - "*"
    /readers:
      - read
```

All repositories support `read` and `write` operations, other specific permissions may be supported
in certain repository types.

Group names should start with `/`, is the example above `read` operation is granted for `readers` group
and every user within the group can read from the repository, user named `jane` is allowed to `read` and `write`.
We also support asterisk wildcard for "any operation" or "any user", user `admin` in the example
can perform any operation in the repository.

If `permissions` section is absent in repo config, then any supported operation is allowed for everyone,
empty `permissions` section restricts any operations for anyone.