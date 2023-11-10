# HTTP 3 Protocol Support

## Server side

Artipie supports http 3 protocol on server side. We use [jetty http3](https://webtide.com/jetty-http-3-support/) 
implementation, currently this implementation is in experimental state. To run repository in http3, add 
the following setting into [repository configuration](./Configuration-Repository):

```yaml
repo:
  type: maven
  storage: default
  port: 5647
  http3: true # enable http3 mode for repository
  http3_ssl:
    jks:
      path: keystore.jks # path to jks file, not storage relative
      password: secret
```

So, to run repository in http3 mode, specify port and `http3` related fields. Http 3 protocol is always secure, so 
SSL setting are required. 

It's possible to start several repositories using http3 on the same port, just specify this same port and `http3` 
related settings for any number of repositories you want.

## Client side (proxy adapters)

To enable http 3 protocol version on client side for proxy adapters, add environmental variable `http3.client` and 
set it to `true`. In this case Artipie will use HTTP/3 protocol for all proxy adapters requests.