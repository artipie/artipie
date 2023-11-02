FROM alpine:3.13
RUN apk --update --no-cache add bash npm
COPY ./run.sh /test/run.sh
COPY ./sample-consumer /test/sample-consumer
COPY ./sample-npm-project /test/sample-npm-project
WORKDIR /test
CMD "/test/run.sh"
