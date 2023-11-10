This example has been created on Windows with Docker version 19.03.8 using the following commands:
```
docker run -d -p 5000:5000 --restart=always -v C:\projects\artipie\docker-example:/var/lib/registry --name registry registry:2

docker pull alpine

docker tag alpine localhost:5000/my-alpine

docker push localhost:5000/my-alpine

docker tag localhost:5000/my-alpine localhost:5000/my-alpine:1
```