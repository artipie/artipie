FROM maven:3.6.3-jdk-13 as build_jar
WORKDIR /jar
RUN yum install git -y
COPY pom.xml /jar/pom.xml
RUN cd /jar && mvn dependency:go-offline
COPY src/ /jar/src/
# @todo #40:30min Propagate git tag
#  I.e. if docker image is based on version 1.5.0, the version should be propagated to the app jar
#  file.
RUN cd /jar && mvn package -B --quiet

FROM adoptopenjdk/openjdk13:alpine-jre
LABEL description="Artipie is a binary repository managment tool."
LABEL maintainer="titantins@gmail.com"
RUN mkdir /artipie
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /artipie/artipie.jar
VOLUME /artipie/repositories
RUN echo -e "meta:\n\
  storage:\n\
    type: fs\n\
    path: /artipie/repositories\n"\
>> /artipie/config.yml
EXPOSE 80
ENTRYPOINT java -jar /artipie/artipie.jar
CMD "--config-file=/artipie/config.yml" "--port=80"
