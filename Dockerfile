FROM maven:3.6.3-jdk-13 as build_jar
RUN yum install git -y
WORKDIR /jar
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ .
# @todo #40 Propagate git tag
#  I.e. if docker image is based on version 1.5.0, the version should be propagated to the app jar
#  file.
RUN mvn package -B --quiet

FROM adoptopenjdk/openjdk13:alpine-jre
LABEL description="Artipie is a binary repository managment tool."
LABEL maintainer="titantins@gmail.com"
WORKDIR /artipie
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /artipie/artipie.jar
VOLUME /artipie/repositories
RUN echo -e "meta:\n\
  storage:\n\
    type: fs\n\
    path: /artipie/repositories\n"\
>> /artipie/config.yml
EXPOSE 80
ENTRYPOINT java -jar /artipie/artipie.jar
CMD --config-file=/artipie/config.yml --port=80