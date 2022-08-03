## Anaconda

[Anaconda](https://repo.anaconda.com/) is a general purpose software repository for Python and other
(R, Ruby, Lua, Scala, Java, JavaScript, C/C++, FORTRAN) packages and utilities, repository short name
is `conda`:
```yaml
repo:
  type: conda
  url: http://{host}:{port}/{repository-name}
  storage:
    type: fs
    path: /var/artipie/my-conda
  settings:
    auth_token_ttl: P30D
    clean_auth_token_at: 0 0 12 * * ?
  permissions:
    alice:
      - upload
      - download
    "*":
      - download
```
Configuration requires `url` field that contains repository full URL,
`{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file).
Anaconda client does not work without authentication and uses tokens to authorize users. That's why
the repository `settings` section contains two optional parameters:
- `auth_token_ttl` is repository authentication tokens time to leave, format is
  compliant with ISO-8601 duration format PnDTnHnMn.nS. Default value is 365 days.
- `clean_auth_token_at` - time to clean expired auth tokens as a cron expression.
  Default value is `0 0 1 * * ?` - at 01 AM every night.

Find more information about permissions [here](../Configuration-Repository%20Permissions.md).

To use Artipie repository with `conda` command-line tool, add the repository to `conda` channels settings to `/root/.condarc` file
(check [documentation](https://conda.io/projects/conda/en/latest/user-guide/configuration/use-condarc.html) for more details):
```yaml
channels:
  - http://{host}:{port}/{repository-name}
```
To install package from the repository, use `conda install`:
```commandline
conda install -y my-package
```
Set Artipie repository url for upload to `anaconda` config and enable automatic upload after building package:
```commandline
anaconda config --set url "http://{host}:{port}/{repository-name}" -s
conda config --set anaconda_upload yes
```
To build and upload the package, login with `anaconda login` first and the then call `conda build`
(the command will build and upload the package to repository):
```commandline
anaconda login --useermane alice --password wanderland
conda build /examle/my-project
```
In the examples above `{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file).