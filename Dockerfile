FROM adoptopenjdk/openjdk14:alpine-jre
ARG ARTIPIE_VERSION=default_val
ARG JAR_FILE
ENV ARTIPIE_VERSION $ARTIPIE_VERSION
ENV JVM_OPTS=""

LABEL description="Artipie binary repository management tool"
LABEL maintainer="g4s8.public@gmail.com"
LABEL maintainer="oleg.mozzhechkov@gmail.com"

RUN addgroup -S -g 2020 artipie && \
    adduser -h /dev/null -D -S -g artipie -u 2021 -s /sbin/nologin artipie && \
    mkdir -p /etc/artipie /usr/lib/artipie /var/artipie && \
    chown artipie:artipie -R /etc/artipie /usr/lib/artipie /var/artipie
USER 2021:2020

COPY target/dependency  /usr/lib/artipie/lib
COPY target/${JAR_FILE} /usr/lib/artipie/artipie.jar

VOLUME /var/artipie /etc/artipie
WORKDIR /var/artipie
EXPOSE 8080
CMD [ \
  "java", \
  "--enable-preview", "-XX:+ShowCodeDetailsInExceptionMessages", \
  "-cp", "/usr/lib/artipie/artipie.jar:/usr/lib/artipie/lib/*", \
  "com.artipie.VertxMain", \
  "--config-file=/etc/artipie/artipie.yml", \
  "--port=8080" \
]
