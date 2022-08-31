## HexPM

### Configuration

HexPM repository is the [package manager for Elixir and Erlang](https://www.hex.pm/) packages.
Here is the configuration example for HexPM repository:
```yaml
# my_hexpm.yaml file
repo:
  type: hexpm
  storage:
    type: fs
    path: /var/artipie/data/
  permissions:
    "*":
      - download
    alice:
       - upload
       - download
```
Repository name is the name of the repo config yaml file(e.g. `my_hexpm`).
Check [storage](../Configuration-Storage.md) and [permission](../Configuration-Repository-Permissions.md) 
documentations to learn more about these settings.

To use your HexPM repository in Elixir project with `mix` build tool, add the following configuration
into `mix.exs` project file (alternatively configure it via [mix hex.config](https://hexdocs.pm/hex/Mix.Tasks.Hex.Config.html) or system environment):
```elixir
# mix.exs file
  def project() do
    [
      # ...
      deps: deps(),
      hex: hex()
    ]
  end
  
  defp deps do
    [
      {:my_artifact, "~> 1.0.0", repo: "my_hexpm"}
    ]
  end

  defp hex() do
    [
      unsafe_registry: true,
      no_verify_repo_origin: true
    ]
  end
```

You must [add repo](https://hexdocs.pm/hex/Mix.Tasks.Hex.Repo.html) to `hex`,
that is directed to your HexPM repository(e.g. name is `my_hexpm`)  with the next command:
```bash
mix hex.repo add <repo_name> http://<artipie_host>:<artipie_port>/<repo_name>
```
```bash
mix hex.repo add my_hexpm http://artipie:8080/my_hexpm
```

To verify that repo has been added, use the following command:
```bash
mix hex.repo list
```

### Fetch dependency

1. To download a package(e.g. **my_artifact** with version **1.0.0**) from `my_hexpm` you can use the command:
```bash
mix hex.package fetch <artifact_name> <version> --repo=<repo_name>
```
```bash
mix hex.package fetch my_artifact 1.0.0 --repo=my_hexpm
```

2. For fetching all dependencies, you can add dependencies in `deps` function in `mix.exs`:
```elixir
# mix.exs file
    def project() do
    [
      # ...
      deps: deps()
    ]
  end

  defp deps do
    [
      {:my_first_artifact, "~> 1.0.1", repo: "my_hexpm"},
      {:my_second_artifact, "~> 2.2.0", repo: "my_hexpm"}
    ]
  end
```
and use the following [command](https://hexdocs.pm/mix/Mix.Tasks.Deps.html):
```bash
mix deps.get
```

### Upload dependency

1. Via rest api

If you have completed tar archive, you can upload it to your HexPM repository with the next command:
```bash
curl -X POST --data-binary "@<path_to_tar>/<tar>" http://<artipie_host>:<artipie_port>/<repo_name>/publish?replace=false
```
```bash
curl -X POST --data-binary "@./decimal-2.0.0.tar" http://artipie:8080/my_hexpm/publish?replace=false
```

If version already exist in your HexPM repository, and you want to replace it, use `true` in query param `replace`:
```bash
curl -X POST --data-binary "@<path_to_tar>/<tar>" http://<artipie_host>:<artipie_port>/<repo_name>/publish?replace=true
```


2. Publish via mix

| ⚠ Note                                                                                                                                                                                                                                                |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| To run this command you need to have an authenticated user on your local machine, run `mix hex.user register` to register or `mix hex.user auth` to authenticate with an existing user, if you already have account at [hexpm](https://hex.pm/login). |


For publish package in your HexPM repository you must change `api_url` in `mix.exs` file:
```elixir
# mix.exs file
  def project() do
    [
      # ...
      hex: hex()
    ]
  end
  
  defp hex() do
    [
      api_url: "http://<artipie_host>:<artipie_port>/<repo_name>"
    ]
  end
```
You can also [override](https://hexdocs.pm/hex/Mix.Tasks.Hex.Config.html#module-config-overrides) it with an environment variable(**HEX_API_URL**) or with `mix hex.config`.

Then you can use the following command to publish artifact of your mix project:
```bash
mix hex.publish package
```