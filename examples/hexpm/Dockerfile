FROM elixir:1.13.4
COPY ./run.sh /test/run.sh
COPY ./sample-consumer /test/sample-consumer
COPY ./sample-for-deployment /test/sample-for-deployment
EXPOSE 8080:8080
WORKDIR /test
CMD "/test/run.sh"
