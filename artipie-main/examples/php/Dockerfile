FROM alpine:3.13
RUN apk --update --no-cache add bash curl zip composer
COPY ./run.sh /test/run.sh
COPY ./sample-consumer /test/sample-consumer
COPY ./sample-for-deployment /test/sample-for-deployment
WORKDIR /test
CMD "/test/run.sh"
