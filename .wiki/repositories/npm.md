## NPM

NPM repository is the [repository for JavaScript](https://www.npmjs.com/) code sharing, packages
store and management. Here is the configuration example for NPM repository:

```yaml
repo:
  type: npm
  url: http://{host}:{port}/{repository-name}
  storage:
    type: fs
    path: /var/artipie/data/
```

The NPM repository configuration requires `url` field that contains repository full URL,
`{host}` and `{port}` are Artipie service host and port, `{repository-name}`
is the name of the repository (and repository name is the name of the repo config yaml file). Check
[storage](./Configuration-Storage) documentations to learn more about storage settings.

Starting with version 8 `npm` client does not work anonymously and requires authorization token. To obtain
the token from Artipie, use Artipie [Rest API](./Rest-api) endpoint from Swagger documentation page 
or simply perform `POST` with `curl` passing user credentials request:
```bash
# request
curl -X POST -d '{"name": "{username}", "pass": "{pswd}"}' \
     -H 'Content-type: application/json' \ 
     http://{host}:{api-port}/api/v1/oauth/token
# response
'{"token": "abc123"}'
```
where `{username}` and `{pswd}` are [user credentials](./Configuration-Credentials), `{host}` and `{api-port}` 
are Artipie service host and Rest API port (default value is 8086). The response is a json with the token. 
This token should be added into `npm` [configuration file `.npmrc`](https://docs.npmjs.com/cli/v9/using-npm/config#_auth) 
with the following line:
```
//{host}:{port}/:_authToken={token}
```
Thus, your `npm` client will use provided token while working with Artipie NPM Registry. 

To use NPM repository with `npm` client, you can specify Artipie NPM repository with `--registry` option:
```bash
# to install the package
npm install @hello/my-project-name --registry http://{host}:{port}/{repository-name}
# to publish the package
npm publish @hello/my-project-name --registry http://{host}:{port}/{repository-name}
```
or it's possible to set Artipie as a default registry:
```bash
npm set registry http://{host}:{port}/{repository-name}
```