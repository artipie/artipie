FROM maven:3.6.3-jdk-13 as build_jar
ARG version="1.0-SNAPSHOT"
WORKDIR /jar
RUN yum install git -y
COPY pom.xml /jar/pom.xml
RUN cd /jar && mvn dependency:go-offline
COPY src/ /jar/src/

RUN cd /jar && mvn versions:set -DnewVersion=${version} && mvn package -B --quiet

FROM adoptopenjdk/openjdk13:alpine-jre
LABEL description="Artipie is a binary repository managment tool."
LABEL maintainer="titantins@gmail.com"
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /usr/lib/artipie.jar
COPY _config.yml /etc/artipie.yml
VOLUME /var/artipie
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/usr/lib/artipie.jar"]
CMD ["--config-file=/etc/artipie.yml", "--port=80"]
