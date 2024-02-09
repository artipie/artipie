FROM alpine:3.19

LABEL description="Docker client for integration tests with java testcontainers"

RUN apk add --update --no-cache openrc docker
RUN rc-status
RUN touch /run/openrc/softlevel
# Insecure registry ports 52001, 52002, 52003
RUN sed -i \
    s/DOCKER_OPTS=/"DOCKER_OPTS=\"--insecure-registry=host.testcontainers.internal:52001 --insecure-registry=host.testcontainers.internal:52002 --insecure-registry=host.testcontainers.internal:52003 \""/g  \
    /etc/conf.d/docker