FROM ruby:3.0.0-alpine3.13
WORKDIR /test
RUN apk --update --no-cache add bash
COPY sample-project /test/sample-project
COPY run.sh /test/
ENTRYPOINT "/test/run.sh"
