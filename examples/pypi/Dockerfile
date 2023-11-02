FROM python:3.9.13-alpine3.15
RUN apk --update --no-cache add bash
COPY ./run.sh /test/run.sh
COPY ./sample-project /test/sample-project
WORKDIR /test
CMD "/test/run.sh"
