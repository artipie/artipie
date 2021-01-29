FROM alpine:3.13
RUN apk add --update --no-cache --virtual .build wget && \
    dir=$(mktemp -d) && \
    wget https://get.helm.sh/helm-v3.5.2-linux-amd64.tar.gz && \
    apk del .build && \
    echo "01b317c506f8b6ad60b11b1dc3f093276bb703281cb1ae01132752253ec706a2 " \
    "helm-v3.5.2-linux-amd64.tar.gz" > helm-v3.5.2-linux-amd64.tar.gz.sha256 && \
    sha256sum -cs helm-v3.5.2-linux-amd64.tar.gz.sha256 && \
    rm helm-v3.5.2-linux-amd64.tar.gz.sha256 && \
    tar -xzf helm-v3.5.2-linux-amd64.tar.gz -C $dir && \
    rm helm-v3.5.2-linux-amd64.tar.gz && \
    install "${dir}/linux-amd64/helm" /bin && \
    rm -rf "$dir"
RUN apk --no-cache add curl bash
COPY ./run.sh ./tomcat-0.4.1.tgz /test/
WORKDIR /test
CMD "/test/run.sh"
