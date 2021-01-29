FROM python:3.9.1-alpine3.13
RUN apk --update --no-cache add bash
COPY ./run.sh /test/run.sh
COPY ./sample-project /test/sample-project
WORKDIR /test
CMD "/test/run.sh"
