2. _**Investigate about public key**_ for repository: _--public-key PATH - Path to public key used to verify the registry_ (optional). [Mix.Tasks.Hex.Repo](https://hexdocs.pm/hex/Mix.Tasks.Hex.Repo.html). That will allow remove `.withEnv("HEX_UNSAFE_REGISTRY", "1")` and `.withEnv("HEX_NO_VERIFY_REPO_ORIGIN", "1")`from `HexITCase.init()` and smoke test in Artipie repository.

3. every time when publish artifact you need enter local machine password.  
Use token or auth with organization

4. when publish - make documentation

5. add support `layout:org`.     .setRepository("artipie") in UploadSlice should be org name

6. metadata.config file contains information about dependency. should we do anything with this information? save to /packages/<package_name> file?
