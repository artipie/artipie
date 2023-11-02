# Implementation details

[Public NPM registry API](https://github.com/npm/registry/blob/master/docs/REGISTRY-API.md) 
describes 6 methods. And one more method is called by `npm` client to get
security audit information. We will support just one in the beginning: get package by name.

## Get package by name method description
Receive request from `npm` client. `{package}` parameter can be in simple (`package-name`) or 
scoped (`@scope/package-name`) form:
```http request
GET /path-to-repository/{package}
connection: keep-alive
user-agent: npm/6.14.3 node/v10.15.2 linux x64
npm-in-ci: false
npm-scope: 
npm-session: 439998791e27a7b1
referer: install
pacote-req-type: packument
pacote-pkg-id: registry:minimalistic-assert
accept: application/vnd.npm.install-v1+json; q=1.0, application/json; q=0.8, */*
accept-encoding: gzip,deflate
Host: artipie.com
```
At first, we try to get `{package}/package.json` file in the storage. If there is no
such file, we send request from `npm-proxy-adapter` to remote registry:
```http request
GET /{package} HTTP/1.1
Host: registry.npmjs.org
Connection: Keep-Alive
User-Agent: Artipie/1.0.0
Accept-Encoding: gzip,deflate
```
Then we handle response from the remote registry. Process headers:
```http request
HTTP/1.1 200 OK
Date: Thu, 02 Apr 2020 11:32:00 GMT
Content-Type: application/vnd.npm.install-v1+json
Content-Length: 14128
Connection: keep-alive
Set-Cookie: __cfduid=d2738100c3ba76fc8be39a390b96a23891585827120; expires=Sat, 02-May-20 11:32:00 GMT; path=/; domain=.npmjs.org; HttpOnly; SameSite=Lax
CF-Ray: 57da3a0fe8ac759b-DME
Accept-Ranges: bytes
Age: 2276
Cache-Control: public, max-age=300
ETag: "c0003ba714ae6ff25985f2b2206a669e"
Last-Modified: Wed, 12 Feb 2020 20:05:40 GMT
Vary: accept-encoding, accept
CF-Cache-Status: HIT
Expect-CT: max-age=604800, report-uri="https://report-uri.cloudflare.com/cdn-cgi/beacon/expect-ct"
Server: cloudflare

{
  "_id": "asdas",
  "_rev": "2-a72e29284ebf401cd7cd8f1aca69af9b",
  "name": "asdas",
  "dist-tags": {
    "latest": "1.0.0"
  },
  "versions": {
    "1.0.0": {
      "name": "asdas",
      "version": "1.0.0",
      "description": "",
      "main": "1.js",
      "scripts": {
        "test": "echo \"Error: no test specified\" && exit 1"
      },
      "author": "",
      "license": "ISC",
      "_id": "asdas@1.0.0",
      "_npmVersion": "5.4.2",
      "_nodeVersion": "8.8.0",
      "_npmUser": {
        "name": "parasoltree",
        "email": "shilijingtian@outlook.com"
      },
      "dist": {
        "integrity": "sha512-kHJzGk3NudKHGhrYS4lhDS8K/QUMbPLEtk22yXiQbcQWD5pSbhOI4A9yk1owav8IVyW1RlAQHkKn7IjONV8Kdg==",
        "shasum": "6470dd80b94c00db02420e5f7bc6a87d026e76e4",
        "tarball": "https://registry.npmjs.org/asdas/-/asdas-1.0.0.tgz"
      },
      "maintainers": [
        {
          "name": "parasoltree",
          "email": "shilijingtian@outlook.com"
        }
      ],
      "_npmOperationalInternal": {
        "host": "s3://npm-registry-packages",
        "tmp": "tmp/asdas-1.0.0.tgz_1511792387536_0.039010856533423066"
      },
      "directories": {},
      "deprecated": "deprecated"
    }
  },
  "readme": "ERROR: No README data found!",
  "maintainers": [
    {
      "name": "parasoltree",
      "email": "shilijingtian@outlook.com"
    }
  ],
  "time": {
    "modified": "2018-12-26T02:15:33.808Z",
    "created": "2017-11-27T14:19:47.631Z",
    "1.0.0": "2017-11-27T14:19:47.631Z"
  },
  "license": "ISC",
  "readmeFilename": ""
}
```
`Last-Modified` header will be persisted to `{package}/package.metadata`,
response body will be persisted to `{package}/package.json`. Fields `tarball` 
in `package.json` have to be modified: we can either re-write all links on 
the initial upload or update them dynamically on each request. The second option is 
better (it allows us to use reverse proxy, for example), but it creates an additional load.

After that we generate response from the `{package}/package.json` and 
`{package}/package.metadata`. We replace placeholders in the `tarball` fields with
actual Artipie repository address and send the following headers:
```http request
HTTP/1.1 200 OK
Date: Thu, 02 Apr 2020 13:54:30 GMT
Server: Artipie/1.0.0
Connection: Keep-Alive
Content-Type: application/json
Content-Length: 14128
Last-Modified: Thu, 12 Mar 2020 18:49:03 GMT

{
  ...
  "versions"."1.0.0"."dist"."tarball": "${artipie.npm_proxy_registry}/asdas/-/asdas-1.0.0.tgz"
  ...
}
```
where `Last-Modified` header is taken from `metadata.json`.

## Get tarball method description
It's the simplified case of the Get package call. We don't need to perform processing 
of the received data. Just need to put tarball in our storage and generate metadata.
Metadata filename is tarball filename with `.metadata` postfix.

Artipie determines the call type (package / tarball) by URL pattern:
* /{package} - Get package call;
* /{package}/-/{package}-{version}.tgz - Get tarball call.

One more thing to keep in mind - it's not required to have existing package metadata 
to process this call.

## Package/tarball not found
If adapter is unable to find the package neither in it's own storage, nor in the 
remote registry, it returns HTTP 404 answer:
```http request
HTTP/1.1 404 Not Found
Date: Thu, 02 Apr 2020 13:54:30 GMT
Server: Artipie/1.0.0
Connection: Keep-Alive
Content-Type: application/json
Content-Length: 21

{"error":"Not found"}
```