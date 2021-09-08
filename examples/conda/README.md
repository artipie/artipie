### Anaconda Repo

![](https://github.com/artipie/artipie/workflows/Proof::conda/badge.svg)

Create new directory `/var/artipie`, directory for configuration files
`/var/artipie/repo` and directory for Anaconda repository `/var/artipie/my-conda`.
Put repository config file to `/var/artipie/repo/my-conda.yaml`:

```yaml
repo:
  type: conda
  url: http://artipie:8080/my-conda
  storage:
    type: fs
    path: /var/artipie/my-debian
```

Do not forget to set repository `url`, it's required by anaconda client API.

Start Artipie Docker image:

```bash
$ docker run -v /var/artipie:/var/artipie artipie/artipie
```

On the client machine add Artipie repository to conda channels settings to `/root/.condarc` file 
(check [documentation](https://conda.io/projects/conda/en/latest/user-guide/configuration/use-condarc.html) for more details):

```yaml
channels:
  - http://artipie:8080/my-conda
```
Set Artipie repository url for upload to anaconda config:
```commandline
anaconda config --set url "http://artipie:8080/my-conda/" -s
```
You can also set automatic upload after building package:
```commandline
conda config --set anaconda_upload yes
``` 

Now you can install packages from Artipie anaconda repository using `conda install` command and 
build and upload packages with `conda build`, or, if the package is already build, 
use `anaconda upload` command to publish package to Artipie.

