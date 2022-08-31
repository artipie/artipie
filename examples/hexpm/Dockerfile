FROM elixir:1.13.4
WORKDIR /test
COPY ./run.sh /test/run.sh
COPY ./sample-consumer /test/sample-consumer
COPY ./sample-for-deployment /test/sample-for-deployment
EXPOSE 8080:8080
RUN chmod +x /test/run.sh
CMD "/test/run.sh"
