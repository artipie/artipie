FROM g4s8/artipie-base:latest as build_jar
ARG version="1.0-SNAPSHOT"
WORKDIR /jar

# Download dependencies
COPY pom.xml ./
RUN mvn dependency:resolve dependency:resolve-plugins clean --fail-never

# Preapare fat jar
COPY src ./src
RUN mvn versions:set -DnewVersion=${version} && \
  mvn package -P assembly -DskipTests

FROM adoptopenjdk/openjdk14:alpine-jre
ENV JVM_OPTS=""
LABEL description="Artipie binary repository managment tool"
LABEL maintainer="titantins@gmail.com"
LABEL maintainer="g4s8.public@gmail.com"
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /usr/lib/artipie.jar
EXPOSE 80
CMD java $JVM_OPTS --enable-preview -XX:+ShowCodeDetailsInExceptionMessages -jar /usr/lib/artipie.jar --config-file=/etc/artipie.yml --port=80
