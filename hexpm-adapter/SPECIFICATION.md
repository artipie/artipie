[Specification](https://github.com/hexpm/specifications/blob/488fdb7e0d92c2149b7d21088621176d3ec76c8d/apiary.apib) + [online viewer](https://dillinger.io/)

[api responses](https://github.com/hexpm/hex/blob/main/test/support/case.ex#L380)

The specifications describe two [endpoints](https://github.com/hexpm/specifications/blob/main/endpoints.md#repository):
1. HTTP API(https://hex.pm/api/) - used for publishing packages, packages search, and administrative tasks.
2. Repository(https://repo.hex.pm/) - read-only endpoint that delivers registry resources and package tarballs.

[hex man](https://medium.com/@toddresudek/hex-power-user-deb608e60935)

[Configure Hex](https://hexdocs.pm/hex/Mix.Tasks.Hex.Config.html) - `mix hex.config`

<hr>

_Mix and Rebar is the building system for Elixir and Erlang, like Maven and Gradle for Java_

[install](https://elixir-lang.org/install.html) erlang and elixir

<hr>

Create new project
```shell
mix new kv --module KV
```

Install hex
```shell
mix local.hex --force
```

**Not require** generate a private key for create hex registry
```shell
openssl genrsa -out private_key.pem
 ```

[self-hosted hex repo](https://hex.pm/docs/self_hosting)

**Not require** create hex registry and it will create `public_key`
```shell
mix hex.registry build public --name=my_repo --private-key=private_key.pem
```
[//]: # (todo how to create public key without making `hex registry`)

my_repo will contain decimal.tar  
Get decimal.tar from hex.pm and move it to your repo
```shell
mix hex.package fetch decimal 2.0.0
```

Add a dependency from Artipie repository(repo name is **_my_repo_**) in `mix.exs` in the `defp deps` function (https://hexdocs.pm/hex/Mix.Tasks.Hex.Repo.html):
```elixir
#mix.exs
defmodule Kv.MixProject do
  use Mix.Project

  def project do
    [
      # ...
      deps: deps()
    ]
  end
  defp deps do
    [
      {:decimal, "~> 2.0.0", repo: "my_repo"}
    ]
  end
end
```

Add repo
```shell
mix hex.repo add my_repo http://<localhost>:<artipie_port>
```

Show all repositories
```shell
mix hex.repo list
```

Switch `unsafe_https` to true (If set to true Hex will not verify HTTPS certificates). Can be overridden by setting the environment variable `HEX_UNSAFE_HTTPS`
```shell
mix hex.config unsafe_https true
```
And switch `no_verify_repo_origin ` to true (If set to true Hex will not verify the registry origin). Can be overridden by setting the environment variable `HEX_NO_VERIFY_REPO_ORIGIN`
```shell
mix hex.config no_verify_repo_origin true
```

Get dependencies with lock version
```shell
mix deps.get
```

Update dependencies bypass of locked version
```shell
mix hex.outdated
mix deps.update
mix deps.update --all
```

<hr>

### fetch dependencies work only with hex.registry
```shell
mix hex.package fetch
mix hex.package fetch decimal 2.0.0 --repo=my_repo
```

<hr>

###  Use it to publish packages in private repo

- [publishing via mix](https://hex.pm/docs/publish)
- [publishing via rebar](https://hex.pm/docs/rebar3_publish)

Publishes a new package version - `POST /publish?replace=<true or false> HTTP_1_1`
```shell
mix hex.publish
```

Publish only package, without docs
```shell
mix hex.publish package
```

Change `api_url` and publish only package
```shell
$ HEX_API_URL=http://<HOST> HEX_API_KEY=<AUTH_KEY> mix hex.publish package
```

push artifact in my_organization's repo - `POST /repos/my_organization/publish?replace=<true or false> HTTP_1_1`
```shell
mix hex.publish --organization my_organization
```
