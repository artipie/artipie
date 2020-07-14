FROM g4s8/artipie-base:latest as build_jar
ARG version="1.0-SNAPSHOT"
WORKDIR /jar
COPY pom.xml ./
COPY src ./src
RUN mvn versions:set -DnewVersion=${version} && \
  mvn package -P assembly

FROM adoptopenjdk/openjdk14:alpine-jre
ENV JAVA_OPTS=""
LABEL description="Artipie binary repository managment tool"
LABEL maintainer="titantins@gmail.com"
LABEL maintainer="g4s8.public@gmail.com"
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /usr/lib/artipie.jar
COPY _config.yml /etc/artipie.yml
EXPOSE 80
ENTRYPOINT [
                "java",
                "$JVM_OPTS",
                "--enable-preview",
                "-XX:+ShowCodeDetailsInExceptionMessages",
                "-jar", "/usr/lib/artipie.jar"
           ]
CMD ["--config-file=/etc/artipie.yml", "--port=80"]
